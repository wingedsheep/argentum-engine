package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kambal, Consul of Allocation
 * {1}{W}{B}
 * Legendary Creature — Human Advisor
 * 2/3
 * Whenever an opponent casts a noncreature spell, that player loses 2 life and you gain 2 life.
 *
 * "That player" is the caster, bound by the opponent-casts trigger as [Player.TriggeringPlayer]
 * (the same binding Mystic Remora's toll uses).
 */
val KambalConsulOfAllocation = card("Kambal, Consul of Allocation") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    oracleText = "Whenever an opponent casts a noncreature spell, that player loses 2 life and you gain 2 life."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.GainLife(2)
        description = "Whenever an opponent casts a noncreature spell, that player loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Vincent Proce"
        flavorText = "He has \"friends\" in places both high and low, and all seem to owe him favors."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d61ddb-2662-427c-a97d-21e41b86130d.jpg?1783937169"
        ruling("2016-09-20", "Kambal's ability resolves before the spell that caused it to trigger.")
        ruling("2016-09-20", "A noncreature spell is a spell that isn't a creature at all. An artifact creature spell isn't a noncreature spell. Lands are never spells.")
    }
}
