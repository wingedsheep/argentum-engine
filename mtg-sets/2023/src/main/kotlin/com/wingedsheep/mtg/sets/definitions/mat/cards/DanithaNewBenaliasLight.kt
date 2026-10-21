package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Danitha, New Benalia's Light — March of the Machine: The Aftermath #29
 * {1}{G}{W} · Legendary Creature — Human Knight · 2/2
 *
 * Vigilance, trample, lifelink
 * Once during each of your turns, you may cast an Aura or Equipment spell from your graveyard.
 *
 * A [MayCastFromGraveyard] grant, same shape as Gisa and Geralf: the spell is cast for its normal
 * cost under its normal timing. `oncePerTurn` tracks the allowance on this permanent, so a new
 * Danitha arriving later in the turn brings a fresh use.
 */
val DanithaNewBenaliasLight = card("Danitha, New Benalia's Light") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Vigilance, trample, lifelink\n" +
        "Once during each of your turns, you may cast an Aura or Equipment spell from your graveyard."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.LIFELINK)

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT),
            duringYourTurnOnly = true,
            oncePerTurn = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Randy Vargas"
        flavorText = "Danitha had spent her whole life preparing to lead House Capashen, but she mourned " +
            "that the opportunity had come so soon."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/039e43f2-cf3b-4c60-ac55-d2aafb20eb34.jpg?1783916517"

        ruling("2023-05-12", "You must follow the normal timing permissions and restrictions of the spell you cast from your graveyard.")
        ruling("2023-05-12", "You must pay the costs to cast that spell. If it has an alternative cost, you may cast it for that cost instead.")
        ruling("2023-05-12", "Once you begin to cast the spell, losing control of Danitha won't affect the spell.")
        ruling("2023-05-12", "If you cast a spell from your graveyard using another permission, Danitha's effect doesn't apply. You can cast another Aura or Equipment spell from your graveyard.")
        ruling("2023-05-12", "If you cast one Aura or Equipment spell from your graveyard and then have a new Danitha come under your control in the same turn, you may cast another Aura or Equipment spell from your graveyard that turn.")
        ruling("2023-05-12", "If an Aura or Equipment card is put into your graveyard during your main phase and the stack is empty, you have a chance to cast it before any player may attempt to remove that card from your graveyard.")
    }
}
