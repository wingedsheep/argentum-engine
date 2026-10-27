package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withoutSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crippling Fear — Kaldheim #82 (canonical printing)
 * {2}{B}{B} · Sorcery
 *
 * Choose a creature type. Creatures that aren't of the chosen type get -3/-3 until end of turn.
 *
 * The type is chosen on resolution; the affected set is snapshotted then (projected subtypes, so a
 * changeling is always spared) and each member gets its own -3/-3, so creatures entering or
 * changing types later in the turn are neither added nor released.
 */
val CripplingFear = card("Crippling Fear") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. Creatures that aren't of the chosen type get -3/-3 until end of turn."

    spell {
        effect = Effects.Pipeline {
            val chosenType = chooseOption(OptionType.CREATURE_TYPE)
            run(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.withoutSubtypeFromVariable(chosenType)),
                    Effects.ModifyStats(-3, -3, EffectTarget.IterationEntity)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Néstor Ossandón Leal"
        flavorText = "Panic spread like wildfire as the demons of Immersturm poured into Bretagard, slaughtering as they came."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d9bd181-b99f-477e-bcfb-9b78cbf51224.jpg?1783928253"
        ruling("2021-02-05", "You choose the creature type as Crippling Fear resolves, and then creatures not of that type get -3/-3. Once Crippling Fear starts to resolve, players can't respond to the choice or take any actions until Crippling Fear finishes resolving.")
        ruling("2021-02-05", "Crippling Fear affects only creatures that aren't of the chosen type that are on the battlefield as it resolves. It won't affect creatures that enter the battlefield later in the turn, regardless of creature type. If a creature changes creature types later in the turn, Crippling Fear won't start or stop affecting that creature.")
    }
}
