package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * It Doesn't Add Up — Murders at Karlov Manor #89
 * {3}{B}{B} · Instant
 *
 * Return target creature card from your graveyard to the battlefield. Suspect it.
 *
 * Instant-speed reanimation, priced with a drawback rather than a mana-value restriction: whatever
 * comes back is suspected, so it has menace and can't block for as long as it stays on the
 * battlefield (CR 701.60a). Reanimating at end of turn to get a surprise blocker therefore does
 * *not* work — the point is to flash in an attacker, or to rebuy a value body.
 *
 * "Suspect it" is not a second target; it names the permanent that just arrived. The reanimated
 * card keeps its entity id across the graveyard → battlefield move, so the same target reference
 * that chose the card in the graveyard addresses the permanent afterwards (same idiom as Prison
 * Break's "with an additional +1/+1 counter on it").
 */
val ItDoesntAddUp = card("It Doesn't Add Up") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to the battlefield. Suspect it. " +
        "(It has menace and can't block.)"

    spell {
        val creatureCard = target(
            "target creature card from your graveyard",
            Targets.CreatureCardInYourGraveyard
        )
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            .then(Effects.Suspect(creatureCard))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Etrata had committed dozens of murders, all of which she recalled in " +
            "perfect, loving detail. But no matter how hard she tried, she couldn't remember " +
            "killing Zegana."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa02dbc2-ad01-47fd-b39e-f0a695029f26.jpg?1783912898"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling(
            "2024-02-02",
            "If a creature is already suspected, suspecting it again won't have any effect."
        )
    }
}
