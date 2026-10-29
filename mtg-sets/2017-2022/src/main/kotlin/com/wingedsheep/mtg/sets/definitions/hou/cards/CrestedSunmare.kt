package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Crested Sunmare
 * {3}{W}{W}
 * Creature — Horse
 * 5/5
 * Other Horses you control have indestructible.
 * At the beginning of each end step, if you gained life this turn, create a 5/5 white Horse
 * creature token.
 *
 * "Other Horses" is the bare subtype noun, so the grant covers every Horse permanent you control
 * (Timber Protector's reading); `excludeSelf` is the "Other". The token trigger fires on every
 * player's end step, gated by the intervening-if [Conditions.YouGainedLifeThisTurn].
 */
val CrestedSunmare = card("Crested Sunmare") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Horse"
    power = 5
    toughness = 5
    oracleText = "Other Horses you control have indestructible.\n" +
        "At the beginning of each end step, if you gained life this turn, create a 5/5 white Horse creature token."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(
                GameObjectFilter.Permanent.youControl().withSubtype("Horse"),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurn
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Horse"),
            imageUri = "https://cards.scryfall.io/normal/front/f/3/f3dd4d92-6471-4f4b-9c70-cbd2196e8c7b.jpg?1783936066"
        )
        description = "At the beginning of each end step, if you gained life this turn, create a 5/5 white Horse creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Lucas Graciano"
        flavorText = "\"It is evidence that some pure corner of the world must still exist.\"\n—Djeru, former Tah-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/732fa4c9-11da-4bdb-96af-aa37c74be25f.jpg?1783936066"
        ruling("2017-07-14", "Crested Sunmare's triggered ability won't trigger unless you've gained life in the turn before the end step began. It can't be satisfied by another triggered ability causing you to gain life during that end step.")
        ruling("2017-07-14", "Crested Sunmare's triggered ability cares only whether you gained life in the turn, even if Crested Sunmare wasn't on the battlefield when that happened. It doesn't care how much you gained, whether you also lost life, or even whether you lost more life than you gained.")
    }
}
