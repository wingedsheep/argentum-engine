package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nightpack Ambusher — the lord bonus is one OR-filter, so a creature that is both a Wolf and a
 * Werewolf gets +1/+1 once. The end-step clause is an intervening "if" read off the whole turn's cast
 * history (a countered spell still counts as cast).
 */
val NightpackAmbusher = card("Nightpack Ambusher") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 4
    toughness = 4
    oracleText = "Flash\n" +
        "Other Wolves and Werewolves you control get +1/+1.\n" +
        "At the beginning of your end step, if you didn't cast a spell this turn, create a 2/2 green Wolf creature token."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf").youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.YouCastSpellsThisTurn(1))
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            controller = EffectTarget.Controller,
            imageUri = "https://cards.scryfall.io/normal/front/b/d/bd05e304-1a16-436d-a05c-4a38a839759b.jpg?1783932856",
        )
        description = "At the beginning of your end step, if you didn't cast a spell this turn, " +
            "create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Dan Murayama Scott"
        flavorText = "A strong leader means a growing pack."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c9b1f70-9861-4c66-a52f-c40002679e75.jpg?1783932960"
        ruling("2019-07-12", "A creature that's both a Wolf and a Werewolf gets only +1/+1 from Nightpack Ambusher's ability.")
        ruling(
            "2019-07-12",
            "Nightpack Ambusher looks at the entire turn to see if you have cast a spell, even if Nightpack Ambusher " +
                "wasn't on the battlefield when that spell was cast. Notably, you won't get a Wolf token during your " +
                "end step if you cast Nightpack Ambusher during your turn.",
        )
        ruling("2019-07-12", "If you cast a spell that was countered, Nightpack Ambusher's last ability doesn't trigger.")
    }
}
