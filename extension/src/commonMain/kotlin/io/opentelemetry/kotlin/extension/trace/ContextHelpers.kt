package io.opentelemetry.kotlin.extension.trace

import io.opentelemetry.kotlin.context.Context
import kotlin.coroutines.CoroutineContext

data class OTelContext(
    val context: Context
): CoroutineContext.Element {
    companion object Key: CoroutineContext.Key<OTelContext>

    override val key: CoroutineContext.Key<OTelContext>
        get() = Key
}


fun CoroutineContext.getOpenTelemetryContext(): Context {
    val element = get(OTelContext.Key)
    if (element is OTelContext) {
        return element.context
    }
    return Context.root()
}

fun Context.asContextElement(): CoroutineContext = OTelContext(this)
