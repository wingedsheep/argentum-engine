package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cosmogrand Zenith
 * {2}{W}
 * Creature — Human Soldier
 * 2/4
 *
 * Whenever you cast your second spell each turn, choose one —
 * • Create two 1/1 white Human Soldier creature tokens.
 * • Put a +1/+1 counter on each creature you control.
 */
val CosmograndZenith = card("Cosmogrand Zenith") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever you cast your second spell each turn, choose one —\n" +
        "• Create two 1/1 white Human Soldier creature tokens.\n" +
        "• Put a +1/+1 counter on each creature you control."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier"),
                    count = 2,
                    imageUri = "https://cards.scryfall.io/normal/front/6/3/631c2c16-132d-4607-ab7e-207a6af188e5.jpg?1757686920"
                ),
                "Create two 1/1 white Human Soldier creature tokens"
            ),
            Mode.noTarget(
                Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                ),
                "Put a +1/+1 counter on each creature you control"
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Anna Steinbauer"
        flavorText = "Cosmogrands of the Celestial Palatinate always seek to expand their grip on the Edge."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c1e5e3-4e6b-456a-958c-7a75c38f8183.jpg?1752946590"
    }
}
