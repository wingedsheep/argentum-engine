package com.wingedsheep.engine.view

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ReidentifiedFromComponent
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Hides, in one viewer's client events, the name of every face-down object that viewer may not
 * look under (issue #2780).
 *
 * An engine event copies a card's name when it is emitted (`CounterAddedEvent.permanentName`,
 * `StatsModifiedEvent.targetName`, the descriptions built from them), and nothing marks that copy
 * as hidden information. Masking each event type by hand left every new one leaking until someone
 * noticed. This pass needs no per-type knowledge instead: an event that names a hidden object by
 * id gets that object's real name replaced, wherever it appears in the event's text, by the label
 * [Visibility.cardNameFor] gives the viewer.
 *
 * It reads the state *after* the events. An object still face down there is still hidden; one
 * that left the battlefield was revealed as it went (CR 708.9), so its name may be shown. A card a
 * face-down shuffle gave a fresh id ([ReidentifiedFromComponent]) is masked under its old ids too.
 */
internal class FaceDownEventMasker(private val visibility: Visibility) {

    fun mask(events: List<ClientEvent>, state: GameState, viewingPlayerId: EntityId): List<ClientEvent> {
        val hidden = hiddenNames(state, viewingPlayerId)
        if (hidden.isEmpty()) return events
        return events.map { event -> maskEvent(event, hidden) }
    }

    /** Each hidden object's id, mapped to its real name and the label the viewer sees instead. */
    private fun hiddenNames(state: GameState, viewingPlayerId: EntityId): Map<String, Pair<Regex, String>> =
        state.entities.keys
            .filter { faceDownDisplayName(state, it) != null }
            .filter { visibility.isCardIdentityHiddenFrom(state, it, viewingPlayerId) }
            .flatMap { id ->
                val entity = state.getEntity(id) ?: return@flatMap emptyList()
                val name = entity.get<CardComponent>()?.name ?: return@flatMap emptyList()
                val label = visibility.cardNameFor(state, id, viewingPlayerId) ?: return@flatMap emptyList()
                if (name == label) return@flatMap emptyList()
                // A card renamed by a face-down shuffle is still named by its old ids in the
                // events of the action that shuffled it.
                val ids = listOf(id) + entity.get<ReidentifiedFromComponent>()?.previousIds.orEmpty()
                ids.map { it.value to (wholeName(name) to label) }
            }
            .toMap()

    private fun maskEvent(event: ClientEvent, hidden: Map<String, Pair<Regex, String>>): ClientEvent {
        val tree = json.encodeToJsonElement(ClientEvent.serializer(), event)
        val mentioned = buildSet { collectStrings(tree, this) }.mapNotNull { hidden[it] }
        if (mentioned.isEmpty()) return event
        val masked = rewriteStrings(tree) { text ->
            mentioned.fold(text) { acc, (name, label) -> name.replace(acc, Regex.escapeReplacement(label)) }
        }
        return json.decodeFromJsonElement(ClientEvent.serializer(), masked)
    }

    private fun collectStrings(element: JsonElement, into: MutableSet<String>) {
        when (element) {
            is JsonObject -> element.values.forEach { collectStrings(it, into) }
            is JsonArray -> element.forEach { collectStrings(it, into) }
            is JsonPrimitive -> if (element.isString) into += element.content
        }
    }

    private fun rewriteStrings(element: JsonElement, rewrite: (String) -> String): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (_, value) -> rewriteStrings(value, rewrite) })
        is JsonArray -> JsonArray(element.map { rewriteStrings(it, rewrite) })
        is JsonPrimitive -> if (element.isString) JsonPrimitive(rewrite(element.content)) else element
    }

    /** [name] as a whole name, so a hidden "Elf" leaves a visible "Elf Warrior" alone. */
    private fun wholeName(name: String) = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(name)}(?![\\p{L}\\p{N}])")

    private companion object {
        val json = Json { encodeDefaults = true }
    }
}
