package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undercity Troll
 * {1}{G}
 * Creature — Troll
 * 2/2
 * Renown 1
 * {2}{G}: Regenerate this creature.
 */
val UndercityTroll = card("Undercity Troll") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll"
    power = 2
    toughness = 2
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n{2}{G}: Regenerate this creature. (The next time this creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"

    keywordAbility(KeywordAbility.renown(1))

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/873d901f-1425-4a0e-a820-015dcf4803f6.jpg?1783938316"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
