package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kai Winn, Faithless Leader
 * {1}{B}
 * Legendary Creature — Bajoran Cleric
 * 3/1
 *
 * As Kai Winn enters, choose a nonland card name.
 * Whenever an opponent casts a spell with the chosen name or activates an ability of a source with
 * the chosen name, that player loses 3 life.
 *
 * The name is the as-enters [EntersWithChoice] `CARD_NAME` slot over the nonland pool (Silverquill
 * Silencer's shape). One printed trigger with two events: [Triggers.or] of a cast and an
 * activation, both filtered by `namedFromChosenComponent()`. "An ability" is unqualified, so mana
 * abilities count (`includeManaAbilities`). A source its own cost sacrificed is matched by its
 * last-known name, as it last existed on the battlefield.
 */
val KaiWinnFaithlessLeader = card("Kai Winn, Faithless Leader") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Bajoran Cleric"
    power = 3
    toughness = 1
    oracleText = "As Kai Winn enters, choose a nonland card name.\n" +
        "Whenever an opponent casts a spell with the chosen name or activates an ability of a source " +
        "with the chosen name, that player loses 3 life."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.anOpponent.casts(GameObjectFilter.Any.namedFromChosenComponent()),
            Triggers.anOpponent.activatesAbility(
                of = GameObjectFilter.Any.namedFromChosenComponent(),
                includeManaAbilities = true,
            ),
        )
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts a spell with the chosen name or activates an ability " +
            "of a source with the chosen name, that player loses 3 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Wei Guan"
        flavorText = "It took masterful insight to lie with the Prophets' own words."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/973ab6cd-fc23-4816-aa08-f77c93e1b1d6.jpg?1793013575"
    }
}
