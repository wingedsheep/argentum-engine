package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gumdrop Poisoner // Tempt with Treats
 * {2}{B}
 * Creature — Human Warlock
 * 3/2
 * Lifelink
 * When this creature enters, up to one target creature gets -X/-X until end of turn, where X is
 * the amount of life you gained this turn.
 *
 * Adventure: Tempt with Treats — {B}, Instant — Adventure
 * Create a Food token.
 *
 * X reads the turn tracker ([DynamicAmounts.lifeGainedThisTurn]) on resolution and is negated via
 * [DynamicAmount.Multiply] by -1, matching the `-X/-X` templating. The Poisoner's own lifelink
 * damage is dealt long after this ETB resolves, so on a clean board X is normally 0 — the tracker
 * counts life gained *earlier* this turn, and a 0 result is a legal no-op rather than a fizzle.
 * The target is `optional = true` ("up to one"), so the ability resolves fine with none chosen.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val GumdropPoisoner = card("Gumdrop Poisoner") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "Lifelink\n" +
        "When this creature enters, up to one target creature gets -X/-X until end of turn, " +
        "where X is the amount of life you gained this turn."
    power = 3
    toughness = 2

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(optional = true, filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(
            DynamicAmount.Multiply(DynamicAmounts.lifeGainedThisTurn(), -1),
            DynamicAmount.Multiply(DynamicAmounts.lifeGainedThisTurn(), -1),
            t
        )
    }

    adventure("Tempt with Treats") {
        manaCost = "{B}"
        typeLine = "Instant — Adventure"
        oracleText = "Create a Food token. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateFood()
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb01d4d-91c2-41c6-981e-b4135a1e1e36.jpg?1783915106"
    }
}
