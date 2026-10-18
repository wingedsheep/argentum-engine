package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Light-Paws, Emperor's Voice — Kamigawa: Neon Dynasty #25
 * {1}{W} · Legendary Creature — Fox Advisor · 2/2
 *
 * Whenever an Aura you control enters, if you cast it, you may search your library for an Aura
 * card with mana value less than or equal to that Aura and with a different name than each Aura
 * you control, put that card onto the battlefield attached to Light-Paws, then shuffle.
 *
 * Modeling notes:
 *  - "if you cast it" is an intervening-if on the *triggering* Aura
 *    ([Conditions.TriggeringEntityWasCast]), so an Aura Light-Paws itself puts onto the
 *    battlefield never re-triggers it.
 *  - The search pool is read at resolution: mana value capped by the triggering Aura
 *    (`manaValueAtMostEntity(TriggeringEntity)`), and the name compared against every Aura you
 *    control at that moment — the triggering one included
 *    (`nameNotSharedWithPermanentYouControl`). Searching for a card with a stated quality may
 *    fail to find (CR 701.23b), so the choice is up to one.
 *  - `move(attachTo = Self)` names the host, so there's no enchant choice; an Aura that can't be
 *    attached to Light-Paws (or with Light-Paws gone) stays in the library, per the ruling.
 */
val LightPawsEmperorsVoice = card("Light-Paws, Emperor's Voice") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Fox Advisor"
    power = 2
    toughness = 2
    oracleText = "Whenever an Aura you control enters, if you cast it, you may search your library for an " +
        "Aura card with mana value less than or equal to that Aura and with a different name than each " +
        "Aura you control, put that card onto the battlefield attached to Light-Paws, then shuffle."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.withSubtype("Aura").youControl()).enters()
        interveningIf = Conditions.TriggeringEntityWasCast
        optional = true
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Enchantment.withSubtype("Aura")
                        .manaValueAtMostEntity(EffectTarget.TriggeringEntity)
                        .nameNotSharedWithPermanentYouControl(GameObjectFilter.Enchantment.withSubtype("Aura"))
                )
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search for an Aura card to put onto the battlefield attached to Light-Paws"
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD), attachTo = EffectTarget.Self)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39555a72-a57b-45ee-9222-ce3b9e8de126.jpg?1791803693"
        ruling(
            "2022-02-18",
            "If the Aura card you find can't be attached to Light-Paws for any reason, it stays in your " +
                "library and does not enter the battlefield."
        )
    }
}
