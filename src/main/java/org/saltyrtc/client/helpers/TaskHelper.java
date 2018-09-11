/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utilities for working with tasks.
 */
public class TaskHelper {

    /**
     * Return list of names of registered tasks.
     */
    public static List<String> getTaskNames(Task[] tasks) {
        return Arrays.stream(tasks)
            .map(Task::getName)
            .collect(Collectors.toList());
    }

    /**
     * Choose the first task in our own list of supported tasks that is also contained in the list
     * of supported tasks provided by the peer.
     *
     * @return The selected task, if a common task can be found.
     */
    public static Optional<Task> chooseCommonTask(@NonNull Task[] ourTasks, @NonNull List<String> theirTasks) {
        return Arrays.stream(ourTasks)
            .filter(task -> theirTasks.contains(task.getName()))
            .findFirst();
    }

}
