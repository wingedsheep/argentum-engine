package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taste of Death — Throne of Eldraine #320
 * {4}{B}{B} · Sorcery
 *
 * Each player sacrifices three creatures of their choice. You create three Food tokens.
 *
 * The Barter in Blood edict shape at `count = 3`: one [Effects.Sacrifice] over [Player.Each].
 * Every player chooses in APNAP order, then all chosen creatures are sacrificed together; a
 * player with three or fewer creatures sacrifices all of them. The Food tokens are created
 * unconditionally, regardless of how many creatures were sacrificed.
 */
val TasteOfDeath = card("Taste of Death") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player sacrifices three creatures of their choice. You create three Food tokens. " +
        "(They're artifacts with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 3,
            target = EffectTarget.PlayerRef(Player.Each)
        ) then Effects.CreateFood(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "320"
        artist = "Chris Rallis"
        flavorText = "\"Hone the knife and mourn the least. Three chimes to sound the sweet, grim feast.\"\n" +
            "—Barrow witch incantation"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/851066e5-d5f2-4e53-8b88-487441a77548.jpg?1783932550"

        ruling(
            "2019-10-04",
            "As Taste of Death resolves, first the player whose turn it is chooses three creatures they " +
                "control, then each other player in turn order does the same, knowing the choices made " +
                "before them. Then all the chosen creatures are sacrificed at the same time."
        )
        ruling(
            "2019-10-04",
            "If a player controls three or fewer creatures, that player sacrifices each creature they control."
        )
        ruling(
            "2019-10-04",
            "You create three Food tokens, no matter how many creatures were actually sacrificed, even if " +
                "some or all players sacrificed no creatures."
        )
    }
}
