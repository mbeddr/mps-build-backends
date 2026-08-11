package de.itemis.mps.gradle.modelcheck

import org.jetbrains.mps.openapi.model.SNode
import org.jetbrains.mps.openapi.persistence.PersistenceFacade
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun encodeQueryComponent(str: String): String =
    // Re-encode characters that are encoded differently in application/x-www-form-urlencoded
    URLEncoder.encode(str, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")

/**
 * A reimplementation of the `getURL` node operation from the mps-httpsupport plugin, to avoid dragging it in as a
 * dependency just for one method.
 *
 * The port is hardcoded to the MPS default of 63320 rather than using whatever port the headless instance would happen
 * to use.
 */
val SNode.url: String
    get() = "http://127.0.0.1:63320/node?ref=${encodeQueryComponent(PersistenceFacade.getInstance().asString(this.reference))}"
