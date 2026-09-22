package io.greencap.k8s.ui;

/**
 * One step of the first-access Tour, as handed to the browser. The targetId is always one of
 * {@link TourTargets}; the renderer resolves it to an element by id.
 */
record TourStep(String targetId, String title, String description) {}
