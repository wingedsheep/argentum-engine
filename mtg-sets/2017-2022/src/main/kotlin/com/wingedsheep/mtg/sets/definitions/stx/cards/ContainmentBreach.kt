package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Containment Breach — Strixhaven: School of Mages #125 (canonical printing)
 * {2}{G} · Sorcery — Lesson
 *
 * Destroy target artifact or enchantment. If its mana value is 2 or less, create a 1/1 black and
 * green Pest creature token with "When this token dies, you gain 1 life."
 *
 * The Perilous Voyage shape: the mana-value check runs *before* the destroy. A target read is
 * live-only, so checking afterwards would read the card in the graveyard — a copy effect (Sculpting
 * Steel copying a two-drop) would show its own printed mana value instead of the one the permanent
 * had on the battlefield (CR 608.2h last-known information). Checking first also gives the ruled
 * result when the permanent survives (indestructible): the Pest is still created. An illegal
 * target fizzles the whole spell, so no Pest then either. The Pest is the predefined Witherbloom
 * token ([Effects.CreatePest]). Lesson is only a subtype.
 */
val ContainmentBreach = card("Containment Breach") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery — Lesson"
    oracleText = "Destroy target artifact or enchantment. If its mana value is 2 or less, create a " +
        "1/1 black and green Pest creature token with \"When this token dies, you gain 1 life.\""

    spell {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.If(
            condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(2), permanent),
            then = Effects.Destroy(permanent) then Effects.CreatePest(),
            otherwise = Effects.Destroy(permanent)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Campbell White"
        flavorText = "For pestkeepers, there's no sound more disheartening than breaking glass."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dcca120-b72e-4007-98e1-cb22e7b9a705.jpg?1783927344"
        ruling("2021-04-16", "If Containment Breach resolves, and the target artifact has a mana value of 2 or less, but it wasn't destroyed (perhaps because it has indestructible), you will still create a Pest.")
        ruling("2021-04-16", "If the artifact or enchantment is an illegal target as Containment Breach tries to resolve, it won't resolve and none of its effects will happen. You won't create a Pest.")
    }
}
