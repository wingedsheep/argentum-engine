package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * The Prydwen, Steel Flagship
 * {4}{W}{W}
 * Legendary Artifact — Vehicle
 * 6/6
 *
 * Flying
 * Whenever another nontoken artifact you control enters, create a 2/2 white Human Knight creature
 * token with "This token gets +2/+2 as long as an artifact entered the battlefield under your
 * control this turn."
 * Crew 2
 *
 * The Knight's anthem is a [ConditionalStaticAbility] on the token itself, gated on
 * [Conditions.ArtifactEnteredBattlefieldThisTurn] — the per-player entry log (Sentinel Sarah Lyons,
 * Mechan Shieldmate), so it keeps the +2/+2 for the rest of the turn even if that artifact later
 * leaves (ruling). "Your" is the token's controller.
 */
val ThePrydwenSteelFlagship = card("The Prydwen, Steel Flagship") {
    manaCost = "{4}{W}{W}"
    typeLine = "Legendary Artifact — Vehicle"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever another nontoken artifact you control enters, create a 2/2 white Human Knight creature " +
        "token with \"This token gets +2/+2 as long as an artifact entered the battlefield under your " +
        "control this turn.\"\n" +
        "Crew 2"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Artifact.youControl().nontoken()).enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Knight"),
            imageUri = "https://cards.scryfall.io/normal/front/5/4/546bda77-e3a8-4f2a-ba9c-94fb0b9ff183.jpg?1783912048",
            staticAbilities = listOf(
                ConditionalStaticAbility(
                    ability = ModifyStats(2, 2, GroupFilter.source()),
                    condition = Conditions.ArtifactEnteredBattlefieldThisTurn,
                ),
            ),
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c0a8eda-bfeb-4fd9-9e41-12fce02883d8.jpg?1783912416"
        ruling(
            "2024-03-08",
            "Once an artifact enters the battlefield under your control in a given turn, it doesn't matter " +
                "what happens to it later. A Knight token created by The Prydwen, Steel Flagship's second " +
                "ability will still get +2/+2 for the rest of that turn."
        )
    }
}
