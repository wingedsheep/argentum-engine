package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tasigur, the Golden Fang
 * {5}{B}
 * Legendary Creature — Human Shaman
 * 4/5
 * Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)
 * {2}{G/U}{G/U}: Mill two cards, then return a nonland card of an opponent's choice from your
 * graveyard to your hand.
 *
 * Modelling notes:
 * - The return is not targeted (ruling): it's a resolution-time selection over your graveyard,
 *   gathered *after* the mill so the two milled cards are eligible — and so is every other nonland
 *   card already there.
 * - [Chooser.Opponent] is "an opponent of your choice decides": with several opponents the
 *   controller first picks which opponent chooses, with one the step is skipped.
 * - The return is mandatory ("return a nonland card", no "may"), so it's `chooseExactly(1)`; with no
 *   nonland card in your graveyard the selection is empty and nothing is returned.
 */
val TasigurTheGoldenFang = card("Tasigur, the Golden Fang") {
    manaCost = "{5}{B}"
    colorIdentity = "UBG"
    typeLine = "Legendary Creature — Human Shaman"
    power = 4
    toughness = 5
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "{2}{G/U}{G/U}: Mill two cards, then return a nonland card of an opponent's choice from your " +
        "graveyard to your hand."

    keywords(Keyword.DELVE)

    activatedAbility {
        cost = Costs.Mana("{2}{G/U}{G/U}")
        effect = Effects.Pipeline {
            // "Mill two cards, ..."
            run(Patterns.Library.mill(2))
            // "... then return a nonland card of an opponent's choice from your graveyard to your hand."
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Nonland))
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.Opponent,
                prompt = "Choose a nonland card from your opponent's graveyard for them to return to their hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        description = "Mill two cards, then return a nonland card of an opponent's choice from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81f93ac5-d149-4ccf-8b99-13ecf3190c29.jpg?1783938691"
        ruling(
            "2021-03-19",
            "The chosen opponent can choose any nonland card in your graveyard, not just one of the milled cards."
        )
        ruling(
            "2021-03-19",
            "Tasigur's last ability doesn't target any card or player. You choose an opponent and that player " +
                "chooses a nonland card only after you mill two cards."
        )
    }
}
