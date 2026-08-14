package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Discerning Financier
 * {2}{W}
 * Creature — Human Noble
 * 2/3
 *
 * At the beginning of your upkeep, if an opponent controls more lands than you, create a Treasure
 * token.
 * {2}{W}: Choose another player. That player gains control of target Treasure you control. You draw
 * a card.
 *
 * The upkeep ability is an intervening-if trigger (CR 603.4) over the shared
 * [Conditions.OpponentControlsMoreLands] land comparison — checked both when the ability would go
 * on the stack and again on resolution, so playing a land in between (or an opponent losing one)
 * turns it off.
 *
 * The activated ability is the donate half of a Treasure payoff: the recipient is *chosen*, not
 * targeted (only the Treasure is a target), so it goes through
 * [Effects.ChooseOpponent] — a resolution-time choice stored in the source's `ChoiceSlot.OPPONENT`
 * — and is read back as [Player.ChosenOpponent] by [GiveControlToTargetPlayerEffect]. The control
 * change is permanent, matching the printed text's lack of a duration. The draw is a separate
 * sentence and so is unconditional, but the whole ability still fizzles if the targeted Treasure
 * is gone on resolution, which also means no card.
 *
 * Oracle "another player" is broader than "an opponent" only in formats with teammates; in a
 * two-player game and in free-for-all multiplayer every other player is an opponent, so the
 * opponent-scoped choice is the faithful modelling for the formats the engine supports.
 */
val DiscerningFinancier = card("Discerning Financier") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Noble"
    power = 2
    toughness = 3
    oracleText = "At the beginning of your upkeep, if an opponent controls more lands than you, " +
        "create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana " +
        "of any color.\")\n" +
        "{2}{W}: Choose another player. That player gains control of target Treasure you control. " +
        "You draw a card."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.OpponentControlsMoreLands
        effect = Effects.CreateTreasure(1)
        description = "At the beginning of your upkeep, if an opponent controls more lands than " +
            "you, create a Treasure token."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        val treasure = target(
            "target Treasure you control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE).youControl()
                )
            )
        )
        effect = Effects.Composite(
            Effects.ChooseOpponent("Choose another player to gain control of the Treasure"),
            GiveControlToTargetPlayerEffect(
                permanent = treasure,
                newController = EffectTarget.PlayerRef(Player.ChosenOpponent)
            ),
            Effects.DrawCards(1)
        )
        description = "Choose another player. That player gains control of target Treasure you " +
            "control. You draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/584774b5-640f-45e0-810b-f5faf119645b.jpg?1783915133"

        ruling(
            "2023-09-01",
            "If the target Treasure is an illegal target as Discerning Financier's last ability " +
                "tries to resolve, it won't resolve. You won't draw a card."
        )
    }
}
