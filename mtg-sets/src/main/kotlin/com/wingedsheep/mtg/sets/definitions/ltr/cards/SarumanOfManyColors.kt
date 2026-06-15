package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Saruman of Many Colors — The Lord of the Rings: Tales of Middle-earth #223
 * {3}{W}{U}{B} · Legendary Creature — Avatar Wizard · 5/4 · Mythic
 *
 * Ward—Discard an enchantment, instant, or sorcery card.
 * Whenever you cast your second spell each turn, each opponent mills two cards. When one or
 * more cards are milled this way, exile target enchantment, instant, or sorcery card with equal
 * or lesser mana value than that spell from an opponent's graveyard. Copy the exiled card. You
 * may cast the copy without paying its mana cost.
 *
 * Composed from existing primitives:
 *  - **Ward—Discard a filtered card** via [KeywordAbility.wardDiscard] now carrying a
 *    [GameObjectFilter] (enchantment OR instant OR sorcery card). The ward executor only counts
 *    matching hand cards toward the can-pay check and offers only matching cards for discard.
 *  - **Second-spell trigger** via [Triggers.NthSpellCast] (n = 2, you).
 *  - **Reflexive after mill** via [ReflexiveTriggerEffect]: the action mills two from each
 *    opponent (storing the milled cards under `"milled"`); the reflexive part is gated on that
 *    collection being non-empty ("when one or more cards are milled this way") and chooses its
 *    target *after* the mill. The target — an enchantment/instant/sorcery card in an opponent's
 *    graveyard with mana value ≤ the triggering (second) spell — is expressed with
 *    `manaValueAtMostEntity(EntityReference.Triggering)`; the trigger's
 *    `triggeringEntityId` is the second spell (still on the stack as the ability resolves above
 *    it), so its mana value is read directly.
 *  - **Copy-a-card-then-cast** via the [Effects.CopyCardIntoCollection] +
 *    [Effects.CastFromCollectionWithoutPayingCost] pattern (same as Shiko, Paragon of the Way):
 *    exile the target, copy it in exile, then `MayEffect`-wrap the free cast. A declined or
 *    uncastable copy is removed by the Rule 707.10a state-based action.
 */
val SarumanOfManyColors = card("Saruman of Many Colors") {
    manaCost = "{3}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 5
    toughness = 4
    oracleText = "Ward—Discard an enchantment, instant, or sorcery card.\n" +
        "Whenever you cast your second spell each turn, each opponent mills two cards. When one " +
        "or more cards are milled this way, exile target enchantment, instant, or sorcery card " +
        "with equal or lesser mana value than that spell from an opponent's graveyard. Copy the " +
        "exiled card. You may cast the copy without paying its mana cost."

    // "enchantment, instant, or sorcery card" — a flat type union (homogeneous OR).
    val enchantmentInstantSorcery =
        GameObjectFilter.Enchantment or GameObjectFilter.Instant or GameObjectFilter.Sorcery

    // Ward—Discard an enchantment, instant, or sorcery card.
    keywordAbility(KeywordAbility.wardDiscard(filter = enchantmentInstantSorcery))

    // Whenever you cast your second spell each turn, each opponent mills two cards.
    // When one or more cards are milled this way, exile target enchantment/instant/sorcery card
    // with mana value ≤ that spell from an opponent's graveyard, copy it, then you may cast it free.
    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        description = "Whenever you cast your second spell each turn, each opponent mills two " +
            "cards. When one or more cards are milled this way, exile target enchantment, " +
            "instant, or sorcery card with equal or lesser mana value than that spell from an " +
            "opponent's graveyard. Copy the exiled card. You may cast the copy without paying " +
            "its mana cost."

        // The exile target is chosen when the reflexive trigger goes on the stack (after the
        // mill), so it is supplied as a reflexive target requirement — NOT an ability-level
        // target — and referenced as ContextTarget(0) in the reflexive effect (Wick's Patrol
        // pattern). "that spell" is the triggering second spell (EntityReference.Triggering).
        val exiledCardTarget = TargetObject(
            filter = TargetFilter(
                baseFilter = enchantmentInstantSorcery
                    .ownedByOpponent()
                    .manaValueAtMostEntity(EntityReference.Triggering),
                zone = Zone.GRAVEYARD,
            )
        )
        val exiledCard = EffectTarget.ContextTarget(0)

        effect = ReflexiveTriggerEffect(
            // Each opponent mills two. Modeled as a flat Gather→Move mill aimed at every opponent
            // (rather than a ForEachPlayer wrapper) so the milled cards surface in the `"milled"`
            // pipeline collection — that collection is what the reflexive's "one or more cards
            // milled this way" gate reads.
            action = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            optional = false,
            // Gate on "one or more cards milled this way": only exile/copy/cast if a card was milled.
            reflexiveEffect = ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("milled"),
                effect = Effects.Composite(
                    Effects.Move(exiledCard, Zone.EXILE),
                    Effects.CopyCardIntoCollection(exiledCard, storeAs = "copy"),
                    MayEffect(
                        Effects.CastFromCollectionWithoutPayingCost("copy"),
                        descriptionOverride = "You may cast the copy without paying its mana cost.",
                    ),
                ),
            ),
            reflexiveTargetRequirements = listOf(exiledCardTarget),
            descriptionOverride = "Each opponent mills two cards. When one or more cards are " +
                "milled this way, exile target enchantment, instant, or sorcery card with equal " +
                "or lesser mana value than that spell from an opponent's graveyard. Copy the " +
                "exiled card. You may cast the copy without paying its mana cost.",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "223"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cfcc7ec-87a2-4712-8d82-217bd8600891.jpg?1686969983"
    }
}
