package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Safehold Sentry
 * {1}{W}
 * Creature — Elf Warrior
 * 2 / 2
 *
 * {2}{W}, {Q}: This creature gets +0/+2 until end of turn. ({Q} is the untap symbol.)
 *
 * - `{Q}` is [Costs.Untap]: the Sentry must already be **tapped** to pay it (so the pump is
 *   normally a follow-up to attacking), and CR 302.6 gates the untap symbol behind summoning
 *   sickness exactly like `{T}`.
 * - "This creature" is the source, so the modifier lands on [EffectTarget.Self] and the ability
 *   takes no target.
 * - [Effects.ModifyStats] defaults to `Duration.EndOfTurn`, the printed duration.
 */
val SafeholdSentry = card("Safehold Sentry") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "{2}{W}, {Q}: This creature gets +0/+2 until end of turn. ({Q} is the untap symbol.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Untap)
        effect = Effects.ModifyStats(0, 2, EffectTarget.Self)
        description = "{2}{W}, {Q}: This creature gets +0/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "William O'Connor"
        flavorText = "\"These bracers were worn by my father and by his mother before him. Boggart fangs have shattered on them. Cinder flames have withered at their touch. While I wear them, the safehold will not fall.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caa8bd74-9897-4515-b1a0-50d5f1bda673.jpg?1783942764"
    }
}
