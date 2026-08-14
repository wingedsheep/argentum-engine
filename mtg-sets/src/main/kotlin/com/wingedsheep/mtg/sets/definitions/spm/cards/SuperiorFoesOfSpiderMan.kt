package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Superior Foes of Spider-Man
 * {2}{R}
 * Creature — Human Rogue Villain, 3/3
 * Trample
 * Whenever you cast a spell with mana value 4 or greater, you may exile the top card of your
 * library. If you do, you may play that card until you exile another card with this creature.
 *
 * Modeling: a `Player.You` cast trigger gated on `manaValueAtLeast(4)` fires a `MayEffect`
 * (the "you may exile" yes/no) wrapping the standard impulse pipeline (gather top card → exile →
 * grant play-from-exile). The permission is granted with [MayPlayExpiry.UntilSourceExilesAnother],
 * which persists across turns (surviving this creature leaving play) but is revoked the moment this
 * same creature exiles another card — faithfully modeling "until you exile another card with this
 * creature": only the most-recently-exiled card stays playable, and any earlier one remains in exile
 * but can no longer be played. The grant is source-scoped to this creature via the trigger's `sourceId`.
 */
val SuperiorFoesOfSpiderMan = card("Superior Foes of Spider-Man") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue Villain"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever you cast a spell with mana value 4 or greater, you may exile the top card of " +
        "your library. If you do, you may play that card until you exile another card with this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4),
                player = Player.You
            ),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(
            Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilSourceExilesAnother)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Ben Harvey"
        flavorText = "Boomerang managed to assemble a squad that really wasn't all that sinister and certainly wasn't all that six."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28e7bf86-5791-4412-8184-fa63fb292be4.jpg?1783905331"
    }
}
