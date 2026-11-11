package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vile Redeemer
 * {2}{G}
 * Creature — Eldrazi
 * 3/3
 * Devoid (This card has no color.)
 * Flash
 * When you cast this spell, you may pay {C}. If you do, create a 1/1 colorless Eldrazi Scion
 * creature token for each nontoken creature that died under your control this turn. Those tokens
 * have "Sacrifice this token: Add {C}."
 *
 * The count is the per-controller nontoken death tracker: a creature counts if it died while under
 * your control, whoever owns it, and wherever the card is now; a creature exiled instead of dying
 * (a replacement effect) never reached the graveyard and isn't counted (2016-01-22 rulings).
 */
val VileRedeemer = card("Vile Redeemer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Flash\n" +
        "When you cast this spell, you may pay {C}. If you do, create a 1/1 colorless Eldrazi Scion " +
        "creature token for each nontoken creature that died under your control this turn. " +
        "Those tokens have \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID, Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.MayPay(
            ManaCost.parse("{C}"),
            Effects.CreateToken(
                count = DynamicAmounts.nonTokenCreaturesDiedThisTurn(Player.You),
                power = 1,
                toughness = 1,
                colors = emptySet(),
                creatureTypes = setOf("Eldrazi", "Scion"),
                activatedAbilities = listOf(
                    ActivatedAbility(
                        cost = Costs.SacrificeSelf,
                        effect = Effects.AddColorlessMana(1),
                        isManaAbility = true,
                        timing = TimingRule.ManaAbility,
                        descriptionOverride = "Sacrifice this token: Add {C}.",
                    )
                ),
                imageUri = "https://cards.scryfall.io/normal/front/a/0/a03edf14-3495-4f61-ad73-f16e9456472c.jpg?1783937863",
            ),
        )
        description = "When you cast this spell, you may pay {C}. If you do, create a 1/1 colorless Eldrazi Scion " +
            "creature token for each nontoken creature that died under your control this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f48b0613-127a-4338-aea4-ad0b8aa96d77.jpg?1783937903"
    }
}
