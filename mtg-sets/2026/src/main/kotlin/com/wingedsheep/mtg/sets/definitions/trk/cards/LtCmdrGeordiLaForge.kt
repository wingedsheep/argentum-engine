package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ScaleCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lt. Cmdr. Geordi La Forge — Star Trek #70
 * {2}{U} · Legendary Creature — Human Artificer · 3/2
 *
 * If you would put one or more charge counters on an artifact you control, put twice that many
 * charge counters on that artifact instead.
 *
 * "If *you* would put" keys on the placer (`placer = Player.You`, Innkeeper's Talent's shape) and,
 * unlike Doubling Season's "if an effect would put", also sees counters put on as a cost
 * (`effectsOnly = false`), so a "put a charge counter on this artifact" cost doubles too.
 */
val LtCmdrGeordiLaForge = card("Lt. Cmdr. Geordi La Forge") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer"
    power = 3
    toughness = 2
    oracleText = "If you would put one or more charge counters on an artifact you control, put " +
        "twice that many charge counters on that artifact instead."

    replacementEffect(
        ScaleCounterPlacement(
            placer = Player.You,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.CHARGE,
                recipient = Recipient.Object(GameObjectFilter.Artifact.youControl()),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Julie Dillon"
        flavorText = "\"There's no way the computer can compensate for the human factor, the . . . " +
            "the intuition, the experience.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a558d82-7efd-49b3-9f7b-ace639b44a35.jpg?1792508185"
    }
}
