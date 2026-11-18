package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boonweaver Giant — Magic 2015 #5
 * {6}{W} · Creature — Giant Monk · 4/4
 *
 * When this creature enters, you may search your graveyard, hand, and/or library for an Aura card
 * and put it onto the battlefield attached to this creature. If you search your library this way,
 * shuffle.
 *
 * - "And/or" is a choice of zones made as the trigger resolves, so it is a [Effects.ChooseAction]
 *   (Basri's Aegis's library-and/or-graveyard shape). The only zone choice with a rules consequence
 *   is whether the library is among them — that alone shuffles and counts as searching a library
 *   (CR 701.23) — so the options are "graveyard, hand, and library", "graveyard and hand" and, for
 *   the "you may", "Don't search". Every Aura reachable from a narrower zone set is in one of the two
 *   pools, so no outcome of the printed choice is lost.
 * - The pool is `couldAttachTo(Self)`: per the ruling, an Aura that can't legally enchant the Giant
 *   can't be put onto the battlefield this way. A hidden zone is in each pool, so the pick is up to
 *   one (failing to find, CR 701.23b).
 * - The move is `attachTo = Self` (Danitha, Benalia's Hope; Auratouched Mage): the Aura enters
 *   attached with no enchant choice, or — with the Giant gone by resolution — stays where it was.
 *   The shuffle still happens when the library was searched, per the second ruling.
 */
val BoonweaverGiant = card("Boonweaver Giant") {
    manaCost = "{6}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Monk"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may search your graveyard, hand, and/or library for an " +
        "Aura card and put it onto the battlefield attached to this creature. If you search your library " +
        "this way, shuffle."

    val aura = GameObjectFilter.Enchantment.withSubtype("Aura").couldAttachTo(EffectTarget.Self)
    val prompt = "Choose an Aura card to put onto the battlefield attached to Boonweaver Giant"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    "Search your graveyard, hand, and library",
                    Effects.Pipeline {
                        val auras = gather(
                            CardSource.FromMultipleZones(
                                zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                                player = Player.You,
                                filter = aura,
                            ),
                            search = true,
                        )
                        val chosen = chooseUpTo(1, from = auras, prompt = prompt)
                        move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = EffectTarget.Self)
                        run(Effects.ShuffleLibrary())
                        // CR 701.23 — the library was searched whether or not anything was found.
                        run(EmitLibrarySearchedEventEffect)
                    }
                ),
                EffectChoice(
                    "Search your graveyard and hand",
                    Effects.Pipeline {
                        val auras = gather(
                            CardSource.FromMultipleZones(
                                zones = listOf(Zone.GRAVEYARD, Zone.HAND),
                                player = Player.You,
                                filter = aura,
                            )
                        )
                        val chosen = chooseUpTo(1, from = auras, prompt = prompt)
                        move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = EffectTarget.Self)
                    }
                ),
                EffectChoice("Don't search", Effects.Nothing),
            )
        )
        description = "When this creature enters, you may search your graveyard, hand, and/or library for an " +
            "Aura card and put it onto the battlefield attached to this creature. If you search your library " +
            "this way, shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85003365-7c5f-4283-995d-e466deef444b.jpg?1783939204"
        ruling(
            "2014-07-18",
            "The Aura card must be able to legally enchant Boonweaver Giant. For example, you can't put an " +
                "Aura with enchant land onto the battlefield this way."
        )
        ruling(
            "2014-07-18",
            "If Boonweaver Giant isn't on the battlefield when its ability resolves, you can't put any Aura " +
                "onto the battlefield. You may still search the appropriate zones and shuffle your library, " +
                "if applicable."
        )
    }
}
