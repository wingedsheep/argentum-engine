package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hikari, Twilight Guardian
 * {3}{W}{W}
 * Legendary Creature — Spirit
 * 4/4
 * Flying
 * Whenever you cast a Spirit or Arcane spell, you may exile Hikari. If you do, return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 *
 * The Spirit-or-Arcane cast trigger (Kami of the Waning Moon's shape) wrapping an optional
 * self-flicker: [Patterns.Exile.exileUntilEndStep] on [EffectTarget.Self] — the exile and a delayed
 * "beginning of the next end step" return, the same composition Astral Slide uses for a target.
 * The trigger resolves before the spell that caused it, so Hikari dodges that spell (ruling).
 */
val HikariTwilightGuardian = card("Hikari, Twilight Guardian") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spirit"
    oracleText = "Flying\nWhenever you cast a Spirit or Arcane spell, you may exile Hikari. If you do, " +
        "return it to the battlefield under its owner's control at the beginning of the next end step."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Spirit", "Arcane"))
        effect = Effects.May(Patterns.Exile.exileUntilEndStep(EffectTarget.Self))
        description = "Whenever you cast a Spirit or Arcane spell, you may exile Hikari. If you do, " +
            "return it to the battlefield under its owner's control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2dc7ba1-6194-45ba-97c5-ad14331cc3a6.jpg?1783944341"
        ruling("2004-12-01", "You may choose not to exile Hikari.")
        ruling(
            "2004-12-01",
            "If you exile Hikari, it leaves the battlefield before the spell that triggered it " +
                "resolves its ability, so it will be unaffected by that spell."
        )
    }
}
