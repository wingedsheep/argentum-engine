package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Eagle of the Great Shelf
 * {4}{W}
 * Creature — Bird Soldier
 * 2/5
 * Flying
 * Whenever this creature attacks, it gets +1/+1 until end of turn for each other creature you control.
 *
 * A one-shot pump, not a static bonus: the count is locked in when the attack trigger resolves
 * (CR 608.2h), so creatures that leave or arrive afterwards don't change the bonus.
 * `excludeSelf` carries the "other" — the Eagle itself never counts.
 */
val EagleOfTheGreatShelf = card("Eagle of the Great Shelf") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    oracleText = "Flying\nWhenever this creature attacks, it gets +1/+1 until end of turn for each other creature you control."
    power = 2
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        val otherCreatures = DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Creature,
            excludeSelf = true
        )
        effect = Effects.ModifyStats(otherCreatures, otherCreatures, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Yuhong Ding"
        flavorText = "The Goblins hated the Eagles and feared them, but could not reach their lofty seats or drive them from the mountains."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3feca644-5f65-4477-bbc8-d505cec6f3a5.jpg?1784797947"
    }
}
