package io.greencap.k8s.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Server-side wrapper for the build-context-picker Web Component.
 * <p>
 * The browser selects a folder, applies the exclusion rules and packs a {@code tar.gz} that arrives
 * here as a single upload; the archive is buffered on temporary disk, never in heap, because a Build
 * Context is orders of magnitude larger than the kubeconfig uploads elsewhere in the platform.
 */
@Slf4j
@Tag("build-context-picker")
@JsModule("./build-context-picker.ts")
public class BuildContextPicker extends Component implements HasSize {

    public record PackedFolder(Path archive, String folderName, int fileCount, long compressedBytes) {
    }

    public static class SelectionChangeEvent extends ComponentEvent<BuildContextPicker> {
        public SelectionChangeEvent(BuildContextPicker source) {
            super(source, true);
        }
    }

    private Path archive;
    private String folderName;
    private int fileCount;
    private long compressedBytes;

    // The resource name must stay "upload": HandlerHelper.isUploadRequest only recognizes
    // ".../<uiId>/<securityKey>/upload" as a framework-internal request, and anything else is left to
    // Spring Security's CSRF filter, which rejects the POST with 403.
    private static final String UPLOAD_RESOURCE_NAME = "upload";

    public BuildContextPicker() {
        getElement().setAttribute("target",
                new StreamReceiver(getElement().getNode(), UPLOAD_RESOURCE_NAME, new ArchiveReceiver()));
    }

    public void setFolderButtonLabel(String label) {
        getElement().setProperty("folderButtonLabel", label);
    }

    public Registration addSelectionChangeListener(ComponentEventListener<SelectionChangeEvent> listener) {
        return addListener(SelectionChangeEvent.class, listener);
    }

    /** The folder ready to be built from — present only once its archive has finished uploading. */
    public Optional<PackedFolder> getPackedFolder() {
        return archive == null || folderName == null ? Optional.empty()
                : Optional.of(new PackedFolder(archive, folderName, fileCount, compressedBytes));
    }

    /** The folder the browser reported packing, whether or not its archive has landed yet. */
    public Optional<String> getFolderName() {
        return Optional.ofNullable(folderName);
    }

    /** Reads a file out of the folder still held by the browser — used to inspect it before building. */
    public void readTextFile(String path, Consumer<String> callback) {
        getElement().callJsFunction("readTextFile", path).then(String.class,
                content -> callback.accept(content == null ? "" : content),
                error -> {
                    log.debug("Could not read {} from the selected folder: {}", path, error);
                    callback.accept("");
                });
    }

    public void clear() {
        discardArchive();
        getElement().callJsFunction("clearSelection");
    }

    @ClientCallable
    public void contextPacked(String folderName, int fileCount, double compressedBytes) {
        this.folderName = folderName;
        this.fileCount = fileCount;
        this.compressedBytes = (long) compressedBytes;
        fireEvent(new SelectionChangeEvent(this));
    }

    @ClientCallable
    public void selectionCleared() {
        discardArchive();
        fireEvent(new SelectionChangeEvent(this));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        discardArchive();
        super.onDetach(detachEvent);
    }

    private void discardArchive() {
        if (archive != null) {
            try {
                Files.deleteIfExists(archive);
            } catch (IOException e) {
                log.warn("Could not delete the temporary build context {}: {}", archive, e.getMessage());
            }
        }
        archive = null;
        folderName = null;
        fileCount = 0;
        compressedBytes = 0;
    }

    private class ArchiveReceiver implements StreamVariable {

        private Path incoming;

        @Override
        public void streamingStarted(StreamingStartEvent event) {
            discardArchive();
            try {
                incoming = Files.createTempFile("greencap-build-context-", ".tar.gz");
            } catch (IOException e) {
                throw new IllegalStateException("Could not create a temporary file for the build context", e);
            }
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return Files.newOutputStream(incoming);
            } catch (IOException e) {
                throw new IllegalStateException("Could not open the temporary build context for writing", e);
            }
        }

        @Override
        public void streamingFinished(StreamingEndEvent event) {
            archive = incoming;
            incoming = null;
        }

        @Override
        public void streamingFailed(StreamingErrorEvent event) {
            log.warn("Build context upload failed: {}", event.getException().getMessage());
            deleteIncoming();
        }

        @Override
        public boolean listenProgress() {
            return false;
        }

        @Override
        public void onProgress(StreamingProgressEvent event) {
            // No progress reporting — the picker reports the packed size before the upload starts.
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        private void deleteIncoming() {
            if (incoming == null) return;
            try {
                Files.deleteIfExists(incoming);
            } catch (IOException e) {
                log.warn("Could not delete the failed build context upload {}: {}", incoming, e.getMessage());
            }
            incoming = null;
        }
    }
}
