package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The gift itself: what the promised opponent receives (CR 702.174d–i).
 *
 * Always addressed to [Player.ChosenOpponent] — the opponent locked into
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT] when the gift cost was paid.
 */
fun giftEffect(kind: GiftKind): Effect {
    val recipient = EffectTarget.PlayerRef(Player.ChosenOpponent)
    return when (kind) {
        GiftKind.CARD -> Effects.DrawCards(1, recipient)
        GiftKind.FOOD -> Effects.CreateFood(1, recipient)
        GiftKind.TREASURE -> Effects.CreateTreasure(1, controller = recipient)
        GiftKind.TAPPED_FISH -> Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Fish"),
            controller = recipient,
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
        )
        GiftKind.OCTOPUS -> Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Octopus"),
            controller = recipient
        )
        GiftKind.EXTRA_TURN -> Effects.TakeExtraTurn(recipient)
    }
}

/**
 * The triggered ability that *is* gift on a permanent (CR 702.174b): "When this permanent enters,
 * if its gift cost was paid, [effect]."
 *
 * An intervening-if trigger gated on [Conditions.GiftWasPromised] (CR 603.4), so a permanent cast
 * without promising the gift never puts the ability on the stack at all. Resolving it is what makes
 * the controller "give a gift" (CR 702.174c), hence the closing [Effects.GiftGiven] marker that
 * fires "whenever you give a gift" triggers.
 *
 * [subject] names the permanent the way the printed card does ("this Aura", "this Equipment") for
 * the rules text only; it defaults to the rule's own generic wording.
 */
fun giftEnterTrigger(kind: GiftKind, subject: String = "this permanent"): TriggeredAbility =
    TriggeredAbility.create(
        trigger = Triggers.EntersBattlefield.event,
        binding = Triggers.EntersBattlefield.binding,
        triggerCondition = Conditions.GiftWasPromised,
        effect = giftEffect(kind).then(Effects.GiftGiven()),
        descriptionOverride =
            "When $subject enters, if the gift was promised, ${kind.effectText}."
    )

/**
 * Add Gift a [kind] (CR 702.174, Bloomburrow) to a **permanent** card — the keyword ability plus
 * the derived enters-the-battlefield gift ability.
 *
 * The promise is an additional cost elected as the spell is cast (CR 702.174a): the legal-action
 * enumerator offers a "promise a gift" cast variant per opponent, the cast handler records the
 * chosen opponent, and the resolving permanent carries
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.GIFT_PROMISED] + `OPPONENT` for the rest of its life.
 * The card's other enters-the-battlefield abilities read that fact through
 * [Conditions.GiftWasPromised] (Scrapshooter's destroy, Starforged Sword's attach) or its negation
 * (Kitnap's stun counters) — never a resolution-time choice, which would ask the player *after*
 * the permanent had already entered.
 *
 * Instants and sorceries have no permanent to trigger off, so their gift branch is folded into the
 * spell's own effect via [MechanicPatterns.giftSpell] instead — `CardValidator` rejects this keyword
 * on a non-permanent for exactly that reason.
 *
 * Call this *after* `typeLine`: the derived ability names the permanent the way the printed card
 * does ("When this Aura enters, …"), read off the type line. Out of order it falls back to the
 * rule's own generic "this permanent" — the wording gets less specific, never wrong.
 */
fun CardBuilder.gift(kind: GiftKind) {
    keywordAbilityList.add(KeywordAbility.Gift(kind))
    triggeredAbilities.add(giftEnterTrigger(kind, giftSubjectFor(typeLine)))
}

/** How the printed card refers to itself in its gift ability, given its type line. */
private fun giftSubjectFor(typeLineString: String): String {
    val typeLine = runCatching { TypeLine.parse(typeLineString) }.getOrNull()
        ?: return "this permanent"
    return when {
        typeLine.isAura -> "this Aura"
        typeLine.isEquipment -> "this Equipment"
        typeLine.isCreature -> "this creature"
        typeLine.isEnchantment -> "this enchantment"
        typeLine.isArtifact -> "this artifact"
        typeLine.isLand -> "this land"
        else -> "this permanent"
    }
}

/**
 * This card's gift keyword, or null when it has none — the single check the engine's cast
 * enumerator, cast handler and stack view use to decide whether "promise a gift" applies.
 */
fun CardDefinition.giftKeyword(): KeywordAbility.Gift? =
    keywordAbilities.filterIsInstance<KeywordAbility.Gift>().firstOrNull()
