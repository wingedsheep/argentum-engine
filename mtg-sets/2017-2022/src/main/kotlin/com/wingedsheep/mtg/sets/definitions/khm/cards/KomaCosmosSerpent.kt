package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Koma, Cosmos Serpent — Kaldheim #221 (canonical printing)
 * {3}{G}{G}{U}{U} · Legendary Creature — Serpent · 6/6
 *
 * This spell can't be countered.
 * At the beginning of each upkeep, create a 3/3 blue Serpent creature token named Koma's Coil.
 * Sacrifice another Serpent: Choose one —
 * • Tap target permanent. Its activated abilities can't be activated this turn.
 * • Koma gains indestructible until end of turn.
 *
 * The lock in the first mode grants the target [PreventActivatedAbilities] over itself until end of
 * turn (Edifice of Authority's shape) — it blocks mana and loyalty abilities too, per the rulings.
 * A modal *ability*, so `countsAsModalSpell = false`.
 */
val KomaCosmosSerpent = card("Koma, Cosmos Serpent") {
    manaCost = "{3}{G}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Serpent"
    power = 6
    toughness = 6
    oracleText = "This spell can't be countered.\n" +
        "At the beginning of each upkeep, create a 3/3 blue Serpent creature token named Koma's Coil.\n" +
        "Sacrifice another Serpent: Choose one —\n" +
        "• Tap target permanent. Its activated abilities can't be activated this turn.\n" +
        "• Koma gains indestructible until end of turn."

    cantBeCountered = true

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Serpent"),
            name = "Koma's Coil",
            imageUri = "https://cards.scryfall.io/normal/front/5/4/54a1c6a9-3531-4432-9157-e4400dbc89fd.jpg?1783928080"
        )
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Any.withSubtype(Subtype.SERPENT))
        effect = ModalEffect.chooseOne(
            mode("Tap target permanent. Its activated abilities can't be activated this turn") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.Tap(permanent) then
                    Effects.GrantStaticAbility(
                        ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                        target = permanent,
                        duration = Duration.EndOfTurn
                    )
            },
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
                "Koma gains indestructible until end of turn"
            ),
            countsAsModalSpell = false
        )
        description = "Sacrifice another Serpent: Choose one — tap target permanent and its activated " +
            "abilities can't be activated this turn; or Koma gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2de25ea4-284a-4c16-b823-048ff00c6a03.jpg?1783928194"
        ruling(
            "2021-02-05",
            "You can't sacrifice Koma itself to activate its last ability, but you can sacrifice any " +
                "other Serpent you control, including Koma's Coil."
        )
        ruling(
            "2021-02-05",
            "If you choose the first mode of Koma's activated ability, you can choose a permanent " +
                "that's already tapped as the target. The effect stopping its activated abilities will " +
                "still apply."
        )
        ruling(
            "2021-02-05",
            "The first mode of Koma's activated ability doesn't stop static abilities from affecting " +
                "the game, and it doesn't stop triggered abilities from triggering. Unlike similar " +
                "effects, it does stop mana abilities from being activated."
        )
    }
}
