package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychogenic Probe — Mirrodin #231
 * {2} · Artifact
 *
 * Whenever a spell or ability causes a player to shuffle their library, this artifact deals
 * 2 damage to that player.
 *
 * A symmetric observer trigger on the shuffle event (CR 701.24), built on
 * [Triggers.WheneverAPlayerShufflesTheirLibrary] — the shuffle twin of the library-search trigger.
 * `Player.Any` is the printed "a player", so the Probe punishes its own controller too, and
 * `Player.TriggeringPlayer` carries "that player" through to the damage.
 *
 * Three rules consequences fall out of the trigger firing once per shuffle rather than once per
 * effect: a fetch land or tutor that searches and then shuffles fires it even though the found
 * cards are held out of the randomization (CR 701.24b); an empty or one-card library still fires
 * it (CR 701.24e, and the 2009 ruling below); and two effects shuffling the same library at once
 * fire it twice (CR 701.24f). Shuffling to set the game up (CR 103.2) and to take a mulligan
 * (CR 103.5) are caused by neither a spell nor an ability, so neither fires it.
 */
val PsychogenicProbe = card("Psychogenic Probe") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a spell or ability causes a player to shuffle their library, " +
        "this artifact deals 2 damage to that player."

    triggeredAbility {
        trigger = Triggers.WheneverAPlayerShufflesTheirLibrary
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Jeremy Jarvis"
        flavorText = "The same devices sold as surgeons' tools in Lumengrid are sold as " +
            "implements of torture in Mephidross."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/" +
            "e83d1ed5-3a49-4cfa-bad2-e342ef28649e.jpg?1783944507"
        ruling(
            "2009-02-01",
            "This ability will trigger even if the player's library is empty at the time they " +
                "are supposed to shuffle it."
        )
    }
}
