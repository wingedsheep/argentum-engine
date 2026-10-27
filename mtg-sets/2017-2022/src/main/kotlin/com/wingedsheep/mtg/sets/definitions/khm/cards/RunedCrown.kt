package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Runed Crown — Kaldheim #245
 * {3} · Artifact — Equipment
 *
 * When this Equipment enters, you may search your library, hand, and/or graveyard for a Rune card
 * and put it onto the battlefield attached to this Equipment. If you search your library this way,
 * shuffle.
 * Equipped creature gets +1/+1.
 * Equip {2}
 *
 * - The enters trigger is the hand-rolled multi-zone search (Vision Quest's shape) over one combined
 *   LIBRARY + HAND + GRAVEYARD pool: `search = true` marks it a library search, the "you may" is
 *   [Effects.May] — declining searches nothing, so nothing is shuffled — and `chooseUpTo(1)` is
 *   failing to find, and the move to the battlefield carries
 *   `attachTo = Self` — Danitha, Benalia's Hope's put-an-Aura-onto-the-battlefield-attached shape.
 *   Every Rune is an "Enchant permanent" Aura, so it can legally enchant the Crown; should the Crown
 *   be gone by resolution, the Aura stays in its previous zone (the attach-to-host move handles it).
 * - The trailing shuffle + library-searched event mirror every other library search.
 */
val RunedCrown = card("Runed Crown") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may search your library, hand, and/or graveyard for a " +
        "Rune card and put it onto the battlefield attached to this Equipment. If you search your " +
        "library this way, shuffle.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val runes = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.LIBRARY, Zone.HAND, Zone.GRAVEYARD),
                        player = Player.You,
                        filter = GameObjectFilter.Any.withSubtype("Rune"),
                    ),
                    search = true,
                )
                val rune = chooseUpTo(
                    1,
                    from = runes,
                    prompt = "Search your library, hand, and/or graveyard for a Rune card to put onto the " +
                        "battlefield attached to Runed Crown",
                )
                move(rune, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = EffectTarget.Self)
                run(Effects.ShuffleLibrary())
                run(EmitLibrarySearchedEventEffect)
            },
            prompt = "Search your library, hand, and/or graveyard for a Rune card?",
        )
        description = "When this Equipment enters, you may search your library, hand, and/or graveyard " +
            "for a Rune card and put it onto the battlefield attached to this Equipment. If you search " +
            "your library this way, shuffle."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ef89770-2dbb-4a57-99f7-191591bb8e38.jpg?1783928183"
    }
}
