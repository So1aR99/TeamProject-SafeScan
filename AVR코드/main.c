#define F_CPU 14745600UL
#include <avr/io.h>
#include <util/delay.h>

// LCD 핀 정의
#define LCD_WDATA PORTC
#define LCD_WINST PORTC
#define LCD_CTRL  PORTB
#define LCD_RS    0
#define LCD_RW    1
#define LCD_EN    2

// 함수 선언
void LCD_Data(char ch);
void LCD_Comm(char ch);
void LCD_CHAR(char c);
void LCD_STR(char *str);
void LCD_pos(char col, char row);
void LCD_Clear(void);
void LCD_Init(void);

void Init_USART(void);
unsigned char USART0_rx(void);
int USART_Available(void);

void setColor(unsigned char r, unsigned char g, unsigned char b);

// ---------------------------- LCD 함수 ----------------------------
void LCD_Data(char ch) {
	LCD_CTRL |= (1<<LCD_RS);
	LCD_CTRL &= ~(1<<LCD_RW);
	LCD_CTRL |= (1<<LCD_EN);
	_delay_us(50);
	LCD_WDATA = ch;
	_delay_us(50);
	LCD_CTRL &= ~(1<<LCD_EN);
}

void LCD_Comm(char ch) {
	LCD_CTRL &= ~(1<<LCD_RS);
	LCD_CTRL &= ~(1<<LCD_RW);
	LCD_CTRL |= (1<<LCD_EN);
	_delay_us(50);
	LCD_WINST = ch;
	_delay_us(50);
	LCD_CTRL &= ~(1<<LCD_EN);
}

void LCD_CHAR(char c){
	LCD_Data(c);
	_delay_ms(1);
}

void LCD_STR(char *str){
	while(*str) LCD_CHAR(*str++);
}

void LCD_pos(char col, char row){
	LCD_Comm(0x80 | (row + col*0x40));
}

void LCD_Clear(void){
	LCD_Comm(0x01);
	_delay_ms(2);
}

void LCD_Init(void){
	LCD_Comm(0x38);
	_delay_ms(2);
	LCD_Comm(0x0E);
	_delay_ms(2);
	LCD_Comm(0x06);
	_delay_ms(2);
	LCD_Clear();
}

// ---------------------------- USART 함수 ----------------------------
void Init_USART(){
	UCSR0B=(1<<RXEN0)|(1<<TXEN0);
	UCSR0C=(1<<UCSZ01)|(1<<UCSZ00);
	UBRR0L=7;
}

unsigned char USART0_rx(){
	while(!(UCSR0A&(1<<RXC0)));
	return UDR0;
}

int USART_Available(){
	return (UCSR0A&(1<<RXC0));
}

// ---------------------------- F-LED PWM 설정 ----------------------------
void setColor(unsigned char r, unsigned char g, unsigned char b){
	// 공통 양극 F-LED 반전 적용
	OCR1AL = 255 - r; // PB5 Red
	OCR1BL = 255 - g; // PB6 Green
	OCR1CL = 255 - b; // PB7 Blue
}

// ---------------------------- 메인 ----------------------------
int main(void)
{
	DDRC = 0xFF; // LCD 데이터/명령 포트 출력
	DDRB = 0xE0; // PB5~7 PWM 출력
	DDRB |= 0x0F; // LCD 제어포트 출력
	ASSR = 0;

	// Phase Correct PWM (8bit, 모드 1)
	TCCR1A = 0xA9;
	TCCR1B = 0x02;
	TCCR1C = 0x00;

	LCD_Init();
	Init_USART();

	LCD_Clear();
	LCD_pos(0,0);
	LCD_STR("Waiting...");

	// 기본 LED OFF
	setColor(0,0,0);

	while(1)
	{
		if(USART_Available())
		{
			char ch = USART0_rx();
			LCD_Clear();
			LCD_pos(0,0);

			if(ch == '1') // 위험 → 빨강 LED
			{
				for(int i=0; i<7; i++) {
					LCD_STR("Warning!!!");
					setColor(255,0,0); // 빨강
					_delay_ms(500);
					LCD_Clear();
					setColor(255,253,85); // 노랑
					_delay_ms(500);
				}
				setColor(0,0,0); // LED OFF
				LCD_Clear();
				LCD_pos(0,0);
				LCD_STR("Waiting...");
			}
			else if(ch == '0') // 안전 → 초록 LED
			{
				LCD_STR("SAFE!!!");
				setColor(0,255,0); // 초록
				_delay_ms(2000);
				setColor(38,255,37);	// 초록색 점점 연해지게 그라데이션(2초 간격)
				_delay_ms(2000);
				setColor(74,255,73);
				_delay_ms(2000);
				setColor(118,255,114);
				_delay_ms(2000);
				setColor(0,0,0); // LED OFF
				LCD_Clear();
				LCD_pos(0,0);
				LCD_STR("Waiting...");
			}
		}
	}
}
