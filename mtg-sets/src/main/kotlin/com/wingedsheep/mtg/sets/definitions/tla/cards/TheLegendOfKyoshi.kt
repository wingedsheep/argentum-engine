package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Legend of Kyoshi // Avatar Kyoshi
 * {4}{G}{G} — Enchantment — Saga
 * //  — Legendary Creature — Avatar 5/4
 *
 * Front — The Legend of Kyoshi:
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I — Draw cards equal to the greatest power among creatures you control.
 *   II — Earthbend X, where X is the number of cards in your hand. That land becomes an Island in
 *        addition to its other types.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Avatar Kyoshi:
 *   Lands you control have trample and hexproof.
 *   {T}: Add X mana of any one color, where X is the greatest power among creatures you control.
 *
 * Chapter I draws cards equal to the MAX power aggregate among your creatures. Chapter II earthbends
 * the targeted land with a dynamic counter count (cards in your hand) via [Effects.Earthbend], then
 * adds the Island subtype additively to that same land. Chapter III is the standard transforming-Saga
 * final chapter — [Effects.ExileAndReturnTransformed] re-enters the permanent as its back face.
 */
private val AvatarKyoshi = card("Avatar Kyoshi") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Creature — Avatar"
    oracleText = "Lands you control have trample and hexproof.\n" +
        "{T}: Add X mana of any one color, where X is the greatest power among creatures you control."
    power = 5
    toughness = 4

    // Lands you control have trample and hexproof.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }

    // {T}: Add X mana of any one color, where X is the greatest power among creatures you control.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
        manaAbility = true
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Thanh Tuấn"
        flavorText = "\"Only justice will bring peace.\""
        imageUri = "https://cards.scryfall.io/normal/back/4/8/4887ce64-7c98-4bd7-95db-0c43ab71cc6e.jpg?1764846544"
    }
}

private val TheLegendOfKyoshiFront = card("The Legend of Kyoshi") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Draw cards equal to the greatest power among creatures you control.\n" +
        "II — Earthbend X, where X is the number of cards in your hand. That land becomes an Island " +
        "in addition to its other types.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    // I — Draw cards equal to the greatest power among creatures you control.
    sagaChapter(1) {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
    }

    // II — Earthbend X (X = cards in your hand). That land becomes an Island in addition to its
    // other types.
    sagaChapter(2) {
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Composite(
            Effects.Earthbend(DynamicAmounts.cardsInYourHand(), land),
            Effects.AddSubtype("Island", land, Duration.Permanent)
        )
    }

    // III — Exile this Saga, then return it to the battlefield transformed under your control.
    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Thanh Tuấn"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4887ce64-7c98-4bd7-95db-0c43ab71cc6e.jpg?1764846544"
    }
}

val TheLegendOfKyoshi: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheLegendOfKyoshiFront,
    backFace = AvatarKyoshi,
)
