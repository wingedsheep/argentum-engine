package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rienne, Angel of Rebirth — Core Set 2020 #281 (canonical printing)
 * {2}{R}{G}{W} · Legendary Creature — Angel · 5/4
 *
 * Flying
 * Other multicolored creatures you control get +1/+0.
 * Whenever another multicolored creature you control dies, return it to its owner's hand at the
 * beginning of the next end step.
 *
 * The lord is a [ModifyStats] over an `.other()` [GroupFilter]. The dies trigger is The Locust
 * God's delayed one-shot: at the next end step it moves the card from the graveyard only, so a
 * card that has already left the graveyard stays where it is (ruling), and the delayed trigger
 * still returns the card if Rienne has left the battlefield by then.
 */
val RienneAngelOfRebirth = card("Rienne, Angel of Rebirth") {
    manaCost = "{2}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Other multicolored creatures you control get +1/+0.\n" +
        "Whenever another multicolored creature you control dies, return it to its owner's hand " +
        "at the beginning of the next end step."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            1,
            0,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()).other()
        )
    }

    triggeredAbility {
        trigger = Triggers.another((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()).dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "281"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fd040a8-b818-465b-a78b-d68402982abc.jpg?1783932925"
        ruling("2019-07-12", "If a multicolored creature you control leaves the graveyard after it dies but before the delayed triggered ability resolves, it will not return to its owner's hand.")
        ruling("2019-07-12", "If an effect causes a non-multicolored creature to become multicolored and that creature dies, the non-multicolored card will return to its owner's hand.")
        ruling("2019-07-12", "If Rienne dies at the same time as one or more other multicolored creatures you control, its last ability triggers for each of those creatures.")
        ruling("2019-07-12", "If Rienne leaves the battlefield after its ability has triggered but before the next end step, its delayed triggered ability will still return the card.")
    }
}
