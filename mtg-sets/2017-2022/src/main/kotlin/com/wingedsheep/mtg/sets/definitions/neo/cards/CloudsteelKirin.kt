package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.GrantStaticAbility

/**
 * Cloudsteel Kirin — Kamigawa: Neon Dynasty #8 (canonical printing)
 * {2}{W} · Artifact Creature — Equipment Kirin · 3/2
 *
 * Flying
 * Equipped creature has flying and "You can't lose the game and your opponents can't win the game."
 * Reconfigure {5}
 *
 * The quoted ability is granted to the equipped creature, so its "you" is that creature's
 * controller — the lock follows the creature (a stolen one protects its new controller) and ends
 * the moment Kirin unattaches. Like Platinum Angel, the two restrictions are two static abilities.
 */
val CloudsteelKirin = card("Cloudsteel Kirin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Equipment Kirin"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Equipped creature has flying and \"You can't lose the game and your opponents can't win the game.\"\n" +
        "Reconfigure {5} ({5}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantStaticAbility(GrantCantLoseGame, filter = Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantStaticAbility(GrantOpponentsCantWinGame, filter = Filters.EquippedCreature)
    }

    reconfigure("{5}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Jonathan Kuo"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a13ddda7-da6e-415e-8886-295514f862d4.jpg?1783923925"

        ruling(
            "2022-02-18",
            "No game effect can cause you to lose the game or cause any opponent to win the game while you " +
                "control a creature equipped with Cloudsteel Kirin. It doesn't matter whether you have 0 or less " +
                "life, you're forced to draw a card while your library is empty, you have ten or more poison " +
                "counters, or so on. You keep playing."
        )
        ruling(
            "2022-02-18",
            "If you have 0 or less life while Cloudsteel Kirin is equipped to a creature, activating reconfigure " +
                "to attach Cloudsteel Kirin to another creature you control doesn't cause you to lose the game. You " +
                "would lose the game if Cloudsteel Kirin becomes unattached or becomes attached to a creature you " +
                "don't control."
        )
    }
}
