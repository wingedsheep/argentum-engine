package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tergrid's Shadow
 * {3}{B}{B}
 * Instant
 * Each player sacrifices two creatures.
 * Foretell {2}{B}{B}
 *
 * The Barter in Blood edict over [Player.Each]: every player chooses in APNAP order, knowing the
 * earlier choices, and then all chosen creatures are sacrificed at once (CR 101.4). A player with
 * two or fewer creatures sacrifices them all without a prompt.
 */
val TergridsShadow = card("Tergrid's Shadow") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each player sacrifices two creatures.\n" +
        "Foretell {2}{B}{B} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 2,
            target = EffectTarget.PlayerRef(Player.Each)
        )
    }

    keywordAbility(KeywordAbility.foretell("{2}{B}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Yongjae Choi"
        flavorText = "Tergrid, god of fright, never acknowledges her own shadow . . . or its murderous deeds."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/417f71d2-d7da-4279-8847-d27c67e9ea9d.jpg?1783928239"

        ruling(
            "2021-02-05",
            "As Tergrid's Shadow resolves, first the player whose turn it is chooses two creatures they " +
                "control, then each other player in turn order does the same, knowing the choices made " +
                "before them. Then all the chosen creatures are sacrificed at the same time."
        )
        ruling("2021-02-05", "If a player controls two or fewer creatures, that player sacrifices each creature they control.")
    }
}
