package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disciple of the Ring
 * {3}{U}{U}
 * Creature — Human Wizard
 * 3/4
 * {1}, Exile an instant or sorcery card from your graveyard: Choose one —
 * • Counter target noncreature spell unless its controller pays {2}.
 * • This creature gets +1/+1 until end of turn.
 * • Tap target creature.
 * • Untap target creature.
 */
val DiscipleOfTheRing = card("Disciple of the Ring") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "{1}, Exile an instant or sorcery card from your graveyard: Choose one —\n" +
        "• Counter target noncreature spell unless its controller pays {2}.\n" +
        "• This creature gets +1/+1 until end of turn.\n" +
        "• Tap target creature.\n" +
        "• Untap target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.InstantOrSorcery)
        )
        effect = ModalEffect.chooseOne(
            mode("Counter target noncreature spell unless its controller pays {2}") {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterUnlessPays("{2}")
            },
            Mode.noTarget(
                Effects.ModifyStats(1, 1, EffectTarget.Self),
                "This creature gets +1/+1 until end of turn"
            ),
            mode("Tap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            },
            mode("Untap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Untap(creature)
            }
        )
        description = "{1}, Exile an instant or sorcery card from your graveyard: Choose one — " +
            "Counter target noncreature spell unless its controller pays {2}; this creature gets +1/+1 " +
            "until end of turn; tap target creature; or untap target creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d344f86-a02d-4de3-82c1-2586dd9ddbfe.jpg?1783938352"
    }
}
