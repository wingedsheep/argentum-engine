package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Madcap Experiment — Kaladesh #122 (canonical printing)
 * {3}{R} · Sorcery
 *
 * Reveal cards from the top of your library until you reveal an artifact card. Put that card onto
 * the battlefield and the rest on the bottom of your library in a random order. Madcap Experiment
 * deals damage to you equal to the number of cards revealed this way.
 *
 * Kethek's walk-and-deploy shape with Spoils of the Vault's count: the damage counts every entity in
 * the revealed collection (the artifact included, per the 2016-09-20 ruling), by id, after the moves.
 * With no artifact in the library the walk reveals it all, nothing enters, the whole library goes to
 * the bottom randomized, and the damage equals the library's size — the other 2016-09-20 ruling.
 */
val MadcapExperiment = card("Madcap Experiment") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Reveal cards from the top of your library until you reveal an artifact card. Put that " +
        "card onto the battlefield and the rest on the bottom of your library in a random order. " +
        "Madcap Experiment deals damage to you equal to the number of cards revealed this way."

    spell {
        effect = Effects.Pipeline {
            val (artifact, revealed) = gatherUntilMatch(GameObjectFilter.Artifact, player = Player.You)
            reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(artifact, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toLibraryBottom(exclude(revealed, artifact), order = CardOrder.Random)
            run(Effects.DealDamage(DynamicAmounts.distinctEntitiesIn(revealed), EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05768b87-d2df-42fc-bf63-e471d31b32e3.jpg?1783937192"
        ruling("2016-09-20", "The number of cards revealed this way includes the artifact card you put onto the battlefield.")
        ruling("2016-09-20", "Once Madcap Experiment begins resolving, no player may take any actions other than those specified by Madcap Experiment until it's done resolving.")
        ruling("2016-09-20", "Any abilities that trigger as the artifact enters the battlefield aren't put onto the stack until Madcap Experiment is done resolving.")
        ruling("2016-09-20", "If you don't reveal an artifact card, you'll randomize your library and be dealt damage equal to the number of cards in your library.")
    }
}
