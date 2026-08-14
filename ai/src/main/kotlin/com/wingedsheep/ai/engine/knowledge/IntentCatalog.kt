package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.state.components.stack.AbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Card-name → [CardIntent] lookup, and the switch that turns Phase 6 on.
 *
 * The AI only ever holds a card *name* (`CardComponent.name`) — resolving that to the
 * [CardDefinition] the analyzer needs takes a [CardRegistry], which only `AIPlayer.create` has. So
 * the catalog is built there, threaded into the consumers, and carried by an
 * [com.wingedsheep.ai.engine.AiProfile] flag.
 *
 * [NONE] is the off position and the default everywhere. It answers `null` to every question, and
 * every consumer falls back to exactly what it did before Phase 6 — which is what keeps
 * `AiProfile.LEGACY_V0`, the permanent reference opponent every published arena number is quoted
 * against, byte-for-byte frozen.
 *
 * Analysis itself is memoized process-wide in [CardIntentAnalyzer], so a catalog is a thin handle:
 * building one per seat per game costs nothing.
 */
class IntentCatalog private constructor(private val registry: CardRegistry?) {

    /** Whether this catalog can answer anything at all. False for [NONE]. */
    val isEnabled: Boolean get() = registry != null

    /**
     * The intent of the card called [name], or null when the catalog is off or the name is not a
     * real card (a token, or a card from a set this registry never loaded).
     *
     * Callers must treat null as "no information" and keep their pre-Phase-6 behaviour, not as
     * "this card does nothing".
     */
    fun forName(name: String): CardIntent? {
        val definition = registry?.getCard(name) ?: return null
        return CardIntentAnalyzer.analyze(definition)
    }

    /**
     * The intent of the face called [faceName] on the card called [cardName], or null when the
     * catalog is off, the name is not a real card, or that card has no such face.
     *
     * What one *face* does is the question a Room asks (CR 709.5): a locked door's text does not
     * exist, so a Room permanent is worth what its unlocked faces do — see [forPermanent].
     */
    fun forFace(cardName: String, faceName: String): CardIntent? {
        val definition = registry?.getCard(cardName) ?: return null
        val face = definition.cardFaces.find { it.name == faceName } ?: return null
        return CardIntentAnalyzer.analyzeFace(definition, face)
    }

    /**
     * Every intent currently *in force* on the battlefield permanent [container], whose card is
     * called [cardName]. Empty when the catalog is off or the name is not a real card — which
     * callers must read as "no information", never as "this permanent does nothing".
     *
     * This is the only reading a permanent should ever be valued by, and it is deliberately the
     * one place that knows a Room from anything else:
     *
     *  - A Room (CR 709.5) contributes one entry per **unlocked** door, because a locked half's
     *    rules text does not exist. Unlocking is what raises the total — and a Room put onto the
     *    battlefield with both doors locked (CR 709.5d) contributes nothing, correctly.
     *  - Anything else contributes its own top-level reading, with the spell faces of an
     *    Adventure / Omen / modal DFC left out: those resolve away to exile, library or graveyard
     *    and are never the permanent standing here.
     *
     * Face membership is tested the same way
     * [com.wingedsheep.engine.state.components.identity.RoomFaceStatics] tests it — by [RoomFaceId]
     * against the card definition's faces — so the two cannot drift on which door is open.
     */
    fun forPermanent(container: ComponentContainer, cardName: String): List<CardIntent> {
        val definition = registry?.getCard(cardName) ?: return emptyList()
        val room = container.get<RoomComponent>()
            ?: return listOf(CardIntentAnalyzer.analyzeSelf(definition))
        return definition.cardFaces
            .filter { RoomFaceId(it.name) in room.unlocked }
            .map { CardIntentAnalyzer.analyzeFace(definition, it) }
    }

    /** The intent of a definition already in hand. Always answers, even on [NONE]. */
    fun forCard(card: CardDefinition): CardIntent = CardIntentAnalyzer.analyze(card)

    /**
     * What the stack object [container] is doing, or null when nothing here can be read.
     *
     * A spell is its card. An **ability** is its effect, which the stack object carries itself — it
     * has no [com.wingedsheep.engine.state.components.identity.CardComponent] at all — so it has to
     * be read from there rather than from the permanent that produced it, which is a different and
     * much broader question.
     *
     * Both readers of the stack want exactly this: [HoldPolicy]'s response window, to decide whether
     * a trick has something it can answer, and [ExpiringGrantWindow]'s, to decide whether the thing
     * on the stack is a reason not to defer. Null is "no information", and both treat it as a reason
     * to decline rather than as "this object is harmless" — see each of their "silence is not a
     * veto" notes.
     */
    fun forStackObject(container: ComponentContainer): CardIntent? {
        val ability = container.get<TriggeredAbilityOnStackComponent>()?.effect
            ?: container.get<ActivatedAbilityOnStackComponent>()?.effect
            ?: container.get<AbilityOnStackComponent>()?.effect
        if (ability != null) return forEffect(ability)
        return container.get<CardComponent>()?.name?.let(::forName)
    }

    /**
     * The ability with [abilityId] printed on the card called [cardName], or null when the catalog
     * is off, the name is not a real card, or no face of it carries that ability.
     *
     * The one reader that wants the ability *itself* rather than a [CardIntent] of it, because
     * [ExpiringGrantWindow] asks two questions no tag carries: how long the effect lasts, and
     * whether the ability can be activated again later this turn.
     *
     * **Printed abilities only.** An ability *granted* to this permanent by something else — an
     * Equipment, an Aura, a `GrantActivatedAbility` — is not on the card definition and answers
     * null, as does a Class whose level gates which abilities exist. Both are the same "no
     * information" every other lookup here returns, and every consumer must keep its pre-flag
     * behaviour on it rather than reading it as "this ability does nothing".
     */
    fun activatedAbility(cardName: String, abilityId: AbilityId): ActivatedAbility? {
        val definition = registry?.getCard(cardName) ?: return null
        definition.script.activatedAbilities.find { it.id == abilityId }?.let { return it }
        return definition.cardFaces.firstNotNullOfOrNull { face ->
            face.script.activatedAbilities.find { it.id == abilityId }
        }
    }

    /**
     * The intent of one ability's [effect] — what a triggered or activated ability sitting on the
     * stack is doing. Always answers, even on [NONE]: an effect needs no registry to read.
     *
     * See [CardIntentAnalyzer.analyzeEffect] for why an ability is read from its effect rather
     * than from the card that produced it.
     */
    fun forEffect(effect: Effect): CardIntent = CardIntentAnalyzer.analyzeEffect(effect)

    companion object {
        /** The off position: no registry, no answers, pre-Phase-6 behaviour everywhere. */
        val NONE = IntentCatalog(null)

        /** A catalog backed by [registry]. */
        fun of(registry: CardRegistry): IntentCatalog = IntentCatalog(registry)
    }
}
