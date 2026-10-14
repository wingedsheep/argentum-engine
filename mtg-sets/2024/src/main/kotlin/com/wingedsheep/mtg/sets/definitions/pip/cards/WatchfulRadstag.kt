package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity

/**
 * Watchful Radstag — Fallout #87.
 *
 * "Whenever this creature evolves" is [Triggers.self]`.evolves()`: the `evolve()` ability emits an
 * `EvolvedEvent` only when its own +1/+1 counter lands (CR 702.100b), so counters from any other
 * source don't copy it, and a Hardened Scales-style extra counter still copies it once.
 */
val WatchfulRadstag = card("Watchful Radstag") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elk Mutant"
    power = 2
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or toughness than this creature, put a +1/+1 counter on this creature.)\nWhenever this creature evolves, create a token that's a copy of it."
    evolve()
    triggeredAbility {
        trigger = Triggers.self.evolves()
        effect = Effects.CreateTokenCopyOfSelf()
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Alexander Ostrowski"
        flavorText = "Don't mistake timidness for defenselessness."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be4151d4-4d35-48d2-9e72-c87014f79137.jpg?1783912393"
        ruling("2024-03-08", "Watchful Radstag “evolves” when its evolve ability resolves and a +1/+1 counter is put on it. If a replacement effect causes the evolve ability to put more than one +1/+1 counter on Watchful Radstag, its last ability triggers only once. If no +1/+1 counter is put on it (perhaps because it left the battlefield while its evolve ability was still on the stack), then its last ability doesn’t trigger.")
        ruling("2024-03-08", "Watchful Radstag’s last ability won’t trigger if a +1/+1 counter is put on it for any reason other than its evolve ability resolving.")
        ruling("2024-03-08", "The token copy will have Watchful Radstag’s abilities. It will also be able to create copies of itself.")
        ruling("2024-03-08", "The token copy won’t copy counters or damage marked on Watchful Radstag, nor will it copy other effects that have changed Watchful Radstag’s power, toughness, types, color, and so on.")
    }
}
