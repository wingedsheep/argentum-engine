package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard

/**
 * Ancient Greenwarden
 * {4}{G}{G}
 * Creature — Elemental
 * 5/7
 * Reach
 * You may play lands from your graveyard.
 * If a land entering causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time.
 *
 * The entering land needn't be yours — only the permanent whose ability triggers must be — so
 * `mustBeYouControl = false`, as Panharmonicon.
 */
val AncientGreenwarden = card("Ancient Greenwarden") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 7
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "You may play lands from your graveyard.\n" +
        "If a land entering causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time."

    keywords(Keyword.REACH)

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Land,
            mustBeYouControl = false,
            description = "If a land entering causes a triggered ability of a permanent you control to " +
                "trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "178"
        artist = "Grzegorz Rutkowski"
        flavorText = "When the ruins awakened, so did their defenses."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe08e59-fdc4-436f-b05c-6ad386c46310.jpg?1783929342"
        ruling(
            "2020-09-25",
            "Ancient Greenwarden doesn't change the times when you can play those lands. You can still play " +
                "only one land per turn, and only during your main phase when you have priority and the stack is empty."
        )
        ruling(
            "2020-09-25",
            "Ancient Greenwarden allows you to play a modal double-faced card's land face, but not a nonland face."
        )
        ruling(
            "2020-09-25",
            "Ancient Greenwarden's third ability affects a land's own enters-the-battlefield triggered abilities " +
                "as well as other triggered abilities that trigger when that land enters the battlefield, such as " +
                "landfall abilities."
        )
        ruling(
            "2020-09-25",
            "Replacement effects are unaffected by Ancient Greenwarden's third ability."
        )
        ruling(
            "2020-09-25",
            "An ability that triggers whenever you play a land won't trigger an additional time."
        )
        ruling(
            "2020-09-25",
            "If you control two Ancient Greenwardens, a land entering the battlefield causes abilities to " +
                "trigger three times, not four."
        )
    }
}
