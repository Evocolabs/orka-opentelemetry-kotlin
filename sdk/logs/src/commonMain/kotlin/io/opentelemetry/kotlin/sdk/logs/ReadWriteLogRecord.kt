/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.log.LogRecordBuilder

/**
 * A combination of the write methods from the [LogRecordBuilder] interface and the read methods
 * from the [ReadableLogRecord] interface.
 *
 * This interface is used internally by the SDK to allow processors to read and potentially
 * modify log records during emission.
 */
interface ReadWriteLogRecord : LogRecordBuilder, ReadableLogRecord
