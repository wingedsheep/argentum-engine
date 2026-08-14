package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thanos, the Mad Titan — Marvel Super Heroes #233 (mythic)
 * {R}{W}{B} · Legendary Creature — Eternal Villain · 4/4
 *
 * Deathtouch, lifelink
 * Power-up — {C}{W}{U}{B}{R}{G}: Put two +1/+1 counters on Thanos. Choose odd or even. Destroy
 * each other creature with mana value of the chosen quality. (Activate each power-up ability only
 * once. Reduce the cost by his mana cost if he entered this turn. Zero is even.)
 *
 * The card that forced power-up's cost reduction to be pip-wise. His activation cost is one of
 * every mana symbol, and his mana cost is three colored pips with no generic at all, so
 * `{C}{W}{U}{B}{R}{G}` − `{R}{W}{B}` = `{C}{U}{G}` — six mana down to three, none of which a
 * generic-only reduction (CR 118.7a) could have touched. See CR 702.193b.
 *
 * "Choose odd or even" is a two-mode [ModalEffect] rather than a bespoke choice type: the choice
 * exists only to pick which sweep happens, which is exactly what a mode is. `manaValueIsOdd()` /
 * `manaValueIsEven()` carry the parity, and the printed "Zero is even" is not a special case here
 * — it's what an even-parity test on 0 already answers.
 *
 * `notSourceItself()` is the printed "each **other** creature" — the bare-GameObjectFilter form of
 * `excludeSelf`, since `DestroyAll` gathers by filter rather than by group. Thanos's own mana value
 * is 3, so without it choosing odd would kill him.
 */
val ThanosTheMadTitan = card("Thanos, the Mad Titan") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Eternal Villain"
    oracleText = "Deathtouch, lifelink\n" +
        "Power-up — {C}{W}{U}{B}{R}{G}: Put two +1/+1 counters on Thanos. Choose odd or even. " +
        "Destroy each other creature with mana value of the chosen quality. (Activate each " +
        "power-up ability only once. Reduce the cost by his mana cost if he entered this turn. " +
        "Zero is even.)"
    power = 4
    toughness = 4

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{C}{W}{U}{B}{R}{G}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            ModalEffect(
                modes = listOf(
                    Mode.noTarget(
                        effect = Effects.DestroyAll(
                            GameObjectFilter.Creature.notSourceItself().manaValueIsOdd()
                        ),
                        description = "Odd — Destroy each other creature with an odd mana value."
                    ),
                    Mode.noTarget(
                        effect = Effects.DestroyAll(
                            GameObjectFilter.Creature.notSourceItself().manaValueIsEven()
                        ),
                        description = "Even — Destroy each other creature with an even mana value."
                    )
                ),
                chooseCount = 1
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "233"
        artist = "Björn Barends"
        flavorText = "\"I shall restore the cosmic balance.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e669c0b2-0011-4feb-9263-f1ecc0a98f18.jpg?1783902896"
    }
}
