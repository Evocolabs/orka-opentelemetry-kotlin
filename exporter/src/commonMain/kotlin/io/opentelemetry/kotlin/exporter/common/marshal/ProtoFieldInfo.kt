package io.opentelemetry.kotlin.exporter.common.marshal

data class ProtoFieldInfo(
    val fieldNumber: Int,
    val tag: Int,
    val tagSize: Int,
    val jsonName: String
)