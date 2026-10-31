package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.plus

/**
 * The card types of CR 205.2a that can appear among permanents or graveyard cards. "Card types
 * among permanents you control and/or cards in your graveyard" is a union, de-duplicated by type,
 * so it counts one per type present in *either* zone rather than adding two per-zone counts.
 */
private val cardTypesAmongPermanentsAndGraveyard = listOf(
    GameObjectFilter.Artifact,
    GameObjectFilter.Battle,
    GameObjectFilter.Creature,
    GameObjectFilter.Enchantment,
    GameObjectFilter.Instant,
    GameObjectFilter.Kindred,
    GameObjectFilter.Land,
    GameObjectFilter.Planeswalker,
    GameObjectFilter.Sorcery,
).map { type ->
    DynamicAmounts.conditional(
        Conditions.Any(Conditions.YouControl(type), Conditions.GraveyardContains(type)),
        1,
        0
    )
}.reduce { total, present -> total + present }

/**
 * Happily Ever After
 * {2}{W}
 * Enchantment
 *
 * When this enchantment enters, each player gains 5 life and draws a card.
 * At the beginning of your upkeep, if there are five colors among permanents you control, there are
 * six or more card types among permanents you control and/or cards in your graveyard, and your life
 * total is greater than or equal to your starting life total, you win the game.
 */
val HappilyEverAfter = card("Happily Ever After") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, each player gains 5 life and draws a card.\n" +
        "At the beginning of your upkeep, if there are five colors among permanents you control, " +
        "there are six or more card types among permanents you control and/or cards in your graveyard, " +
        "and your life total is greater than or equal to your starting life total, you win the game."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(5, EffectTarget.PlayerRef(Player.Each)) then
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.All(
            Conditions.CompareAmounts(DynamicAmounts.colorsAmongPermanents(), ComparisonOperator.GTE, 5),
            Conditions.CompareAmounts(cardTypesAmongPermanentsAndGraveyard, ComparisonOperator.GTE, 6),
            Conditions.CompareAmounts(
                DynamicAmounts.yourLifeTotal(),
                ComparisonOperator.GTE,
                DynamicAmounts.startingLifeTotal()
            )
        )
        effect = Effects.WinGame(target = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d32d85d5-a6f0-4cc5-9fd6-6b329aae2e5b.jpg?1783932672"

        ruling("2019-10-04", "The second triggered ability of Happily Ever After has one very long intervening \"if\" clause. This means that all of the conditions must be true as your upkeep begins—you must control permanents with all five colors, your permanents and graveyard must contain six or more card types, and your life total must be high enough—or the ability won't trigger at all. If they're not all true as the ability resolves, you won't win the game.")
        ruling("2019-10-04", "You could have one permanent that's all five colors, or the five colors could be spread out among any number of permanents.")
        ruling("2019-10-04", "Lands are normally colorless permanents, even if they tap for mana of a certain color.")
        ruling("2019-10-04", "The card types that could appear among your permanents and cards in your graveyard are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Supertypes, such as legendary and basic, aren't card types.")
        ruling("2019-10-04", "Ignore the alternative characteristics of an adventurer card in your graveyard. Its alternative card type won't be counted among the card types of cards in your graveyard.")
    }
}
