package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Rulik Mons, Warren Chief
 * {1}{R}{G}{G}
 * Legendary Creature — Goblin
 * 3/3
 *
 * Menace
 * Whenever Rulik Mons attacks, look at the top card of your library. If it's a land card, you may
 * put it onto the battlefield tapped. If you didn't put a card onto the battlefield this way,
 * create a 1/1 red Goblin creature token.
 *
 * The attack trigger is an [Effects.IfYouDo] whose action is Explorer's Scope's top-card pipeline
 * (a non-land, or a declined land, stays on top). [SuccessCriterion.Auto] scores it on the
 * terminal move actually growing your battlefield, so a non-land top card, an empty library, or
 * declining the land all fall to `otherwise` — the Goblin (the 2022-09-09 ruling).
 */
val RulikMonsWarrenChief = card("Rulik Mons, Warren Chief") {
    manaCost = "{1}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Goblin"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever Rulik Mons attacks, look at the top card of your library. If it's a land card, " +
        "you may put it onto the battlefield tapped. If you didn't put a card onto the battlefield " +
        "this way, create a 1/1 red Goblin creature token."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(1))
                val landCards = filter(looked, GameObjectFilter.Land)
                val toBattlefield = chooseUpTo(1, from = landCards, selectedLabel = "Put onto the battlefield tapped")
                move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            },
            then = Effects.Nothing,
            otherwise = Effects.CreateToken(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                imageUri = "https://cards.scryfall.io/normal/front/1/2/128511d0-bc4c-48d9-9e8e-9c7c945cfaeb.jpg?1783921128",
            ),
            successCriterion = SuccessCriterion.Auto,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fa49711-473d-4774-99b8-2bfbabc6b735.jpg?1783921277"

        ruling("2022-09-09", "Putting a land onto the battlefield with Rulik Mons, Warren Chief's second ability doesn't count as playing a land for the turn.")
        ruling("2022-09-09", "If it is a land, you may choose not to put it onto the battlefield this way. In that case, you will create a 1/1 red Goblin creature token.")
    }
}
