package io.opentelemetry.kotlin.api.common

/**
 * Value mirrors the proto <a
 * href="https://github.com/open-telemetry/opentelemetry-proto/blob/ac3242b03157295e4ee9e616af53b81517b06559/opentelemetry/proto/common/v1/common.proto#L28">AnyValue</a>
 * message type, and is used to model any type.
 *
 * <p>It can be used to represent:
 *
 * <ul>
 *   <li>Primitive values via {@link #of(long)}, {@link #of(String)}, {@link #of(boolean)}, {@link
 *       #of(double)}.
 *   <li>String-keyed maps (i.e. associative arrays, dictionaries) via {@link #of(KeyValue...)},
 *       {@link #of(Map)}. Note, because map values are type {@link Value}, maps can be nested
 *       within other maps.
 *   <li>Arrays (heterogeneous or homogenous) via {@link #of(Value[])}. Note, because array values
 *       are type {@link Value}, arrays can contain primitives, complex types like maps or arrays,
 *       or any combination.
 *   <li>Raw bytes via {@link #of(byte[])}
 * </ul>
 *
 * <p>Currently, Value is only used as an argument for {@link
 * io.opentelemetry.api.logs.LogRecordBuilder#setBody(Value)}.
 *
 * @param <T> the type. See {@link #getValue()} for description of types.
 * @since 1.42.0
 */
interface Value<T> {
    val type: ValueType
    val value: T

    fun asString(): String
}