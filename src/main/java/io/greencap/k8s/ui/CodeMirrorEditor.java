package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@Tag("code-mirror-editor")
@NpmPackage(value = "codemirror", version = "6.0.1")
@NpmPackage(value = "@codemirror/lang-yaml", version = "6.1.1")
@NpmPackage(value = "@codemirror/theme-one-dark", version = "6.1.2")
@JsModule("./code-mirror-editor.ts")
public class CodeMirrorEditor extends Component implements HasSize {

    public CodeMirrorEditor() {
        getElement().addPropertyChangeListener("value", "value-changed", e -> {});
    }

    public String getValue() {
        return getElement().getProperty("value", "");
    }

    public void setValue(String value) {
        getElement().setProperty("value", value != null ? value : "");
    }

    public void setReadOnly(boolean readOnly) {
        getElement().setProperty("readOnly", readOnly);
    }

    public void focus() {
        getElement().callJsFunction("focus");
    }
}
