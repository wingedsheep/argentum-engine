package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Insult // Injury — Amonkhet #213
 *
 * Insult {2}{R} — Sorcery: Damage can't be prevented this turn. If a source you control would deal
 *   damage this turn, it deals double that damage instead.
 * Injury {2}{R} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Injury deals 2 damage to target creature and 2 damage to target player or planeswalker.
 *
 * Insult installs two turn-scoped effects that outlive the spell: the game-wide "can't be prevented"
 * and an until-end-of-turn doubling replacement (CR 616) scoped to sources Insult's controller
 * controls. Each Insult doubles once, so two of them quadruple.
 */
val InsultInjury = card("Insult // Injury") {
    layout = CardLayout.SPLIT
    colorIdentity = "R"

    face("Insult") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        oracleText = "Damage can't be prevented this turn. If a source you control would deal damage this turn, " +
            "it deals double that damage instead."

        spell {
            effect = Effects.DamageCantBePreventedThisTurn() then
                Effects.DoubleDamageThisTurn(EventPattern.DamageEvent(source = GameObjectFilter.Any.youControl()))
        }
    }

    face("Injury") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Injury deals 2 damage to target creature and 2 damage to target player or planeswalker."
        keywords(Keyword.AFTERMATH)

        spell {
            val creature = target(TargetFilter.Creature)
            val victim = target(Targets.PlayerOrPlaneswalker)
            effect = Effects.DealDamage(2, creature) then Effects.DealDamage(2, victim)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeac671f-2606-43ed-ad60-a69df5c150f6.jpg?1783936458"
        ruling("2017-04-18", "If an effect such as that of Chandra's Pyrohelix asks you to divide damage among targets, you must divide the unmodified damage before doubling it.")
        ruling("2017-04-18", "If you cast two Insults in one turn, damage dealt by sources you control this turn will be multiplied by 4. If you cast three Insults, it will be multiplied by 8, and so on. How rude.")
        ruling("2017-04-18", "If a creature with trample you control would deal combat damage to a blocking creature this turn, you must assign its unmodified damage. For example, a 3/3 creature with trample blocked by a 2/2 creature can only have 1 damage assigned to the defending player. It will then deal 4 damage to the blocking creature and 2 to the defending player.")
        ruling("2017-04-18", "You can't cast Injury unless you target both a creature and a player. If one target is illegal as Injury resolves, the spell deals damage to the remaining legal target.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
