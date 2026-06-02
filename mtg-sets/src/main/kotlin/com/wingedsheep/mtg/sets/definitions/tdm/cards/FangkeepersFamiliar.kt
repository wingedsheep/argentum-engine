package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Fangkeeper's Familiar
 * {1}{B}{G}{U}
 * Creature — Snake
 * 3/3
 *
 * Flash
 * When this creature enters, choose one —
 * • You gain 3 life and surveil 3.
 * • Destroy target enchantment.
 * • Counter target creature spell.
 *
 * The ETB ability is modal (choose one) via [ModalEffect.chooseOne]; each mode carries its own
 * targeting. Flash lets it enter at instant speed, making the "counter target creature spell"
 * mode meaningful.
 */
val FangkeepersFamiliar = card("Fangkeeper's Familiar") {
    manaCost = "{1}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "When this creature enters, choose one —\n" +
        "• You gain 3 life and surveil 3.\n" +
        "• Destroy target enchantment.\n" +
        "• Counter target creature spell."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.GainLife(3).then(LibraryPatterns.surveil(3)),
                "You gain 3 life and surveil 3"
            ),
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Enchantment,
                "Destroy target enchantment"
            ),
            Mode.withTarget(
                Effects.CounterSpell(),
                Targets.CreatureSpell,
                "Counter target creature spell"
            )
        )
        description = "When this creature enters, choose one — You gain 3 life and surveil 3; " +
            "or destroy target enchantment; or counter target creature spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "David Szabo"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/655fa2e1-3e1c-424c-b17a-daa7b8fface4.jpg?1743204714"
    }
}
