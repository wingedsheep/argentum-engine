package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Minister of Pain
 * {2}{B}
 * Creature — Human Shaman
 * 2/3
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, creatures your opponents control get -1/-1 until end of turn.
 *
 * The payoff is Plague Mare's sweep — a [Effects.ForEachInGroup] over the opponents' creatures
 * applying -1/-1 to each — riding the exploit reflexive.
 */
val MinisterOfPain = card("Minister of Pain") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 3
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, creatures your opponents control get -1/-1 until end of turn."

    exploit(
        onExploit = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Izzy"
        flavorText = "Draconic words need not be shouted. A whisper will suffice."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cf4c637-82c3-41fa-b484-31e7dcf475a9.jpg"
    }
}
