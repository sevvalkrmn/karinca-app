"""The narrate() boundary. This is the ONLY place that talks to the LLM, and the
only inputs it ever sees are ReportFacts (already-computed numbers) and
ProfileSnapshot (tone + reference target) — never raw transactions, never a name,
never anything the FinancialEngine didn't already put in ReportFacts. Swapping
providers means changing this file only; FinancialEngine never changes.

Single structured prompt call — one system message, one user message, no tool use,
no multi-turn chain.
"""

from openai import OpenAI

from app.models import ProfileSnapshot, ReportFacts

TONE_INSTRUCTIONS = {
    "gentle": "Çok nazik ve destekleyici ol, asla suçlayıcı bir dil kullanma; küçük "
    "iyileşmeleri bile fark edip öv.",
    "balanced": "Dengeli ve dürüst ol — hem övgü hem yapıcı eleştiri yap, ama her "
    "zaman saygılı kal.",
    "brutal": "Doğrudan ve sert ol, laf sokabilirsin; ama küçük düşürücü ya da "
    "hakaret içeren bir dile asla kaçma — esprili bir sertlik hedefle.",
}

SYSTEM_PROMPT = """Sen Karınca — tutumlu, esprili ama sıcak bir karınca maskotu, kullanıcının kişisel \
finans koçusun. "Karınca kararınca" ilkesine inanırsın: küçük, tutarlı tasarruf zamanla \
büyük fark yaratır. Kullanıcıya "sen" diye hitap et. Sadece Türkçe yaz, düz metin \
olsun (markdown, madde işareti, emoji kullanma). 80-150 kelime arasında kal.

KURAL (asla çiğneme): Sadece aşağıda JSON olarak verilen FACTS içindeki sayıları \
kullanabilirsin. Hiçbir sayı uydurma, tahmin etme veya farklı yuvarlama ile verme. \
FACTS'te olmayan bir rakamdan asla bahsetme. Bir alan eksikse/null ise, o konudan \
hiç söz etme — var olmayan bir karşılaştırma veya öneri icat etme."""


def narrate(facts: ReportFacts, profile: ProfileSnapshot, api_key: str, model: str) -> str:
    client = OpenAI(api_key=api_key)
    tone_instruction = TONE_INSTRUCTIONS[profile.coach_tone]
    user_prompt = (
        f"TON: {profile.coach_tone} — {tone_instruction}\n\n"
        f"PROFİL: referans tipi = {profile.reference_type}, "
        f"referans tutarı = {profile.reference_amount:g} TL\n\n"
        f"FACTS (JSON):\n{facts.model_dump_json(indent=2, exclude_none=True)}\n\n"
        "Bu haftanın karınca-koçu raporunu yaz. FACTS'te varsa geçmişe dönük bir "
        "kıyas (vs_last_week) ve en az bir ileriye dönük tasarruf önerisi "
        "(savings_suggestion) içersin."
    )
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.7,
        max_tokens=400,
    )
    content = response.choices[0].message.content
    return content.strip() if content else ""
